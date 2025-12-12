package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.event.ChatEventBus
import com.okestro.okchat.chat.event.ChatInteractionCompletedEvent
import com.okestro.okchat.chat.event.ConversationHistorySaveEvent
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.CompleteChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipeline
import com.okestro.okchat.chat.service.dto.ChatServiceRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import org.slf4j.MDC
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.UUID
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

/**
 * Main chat service for handling user queries
 * Uses a pipeline-based architecture for extensibility
 * Supports multi-turn conversations with session management
 *
 * Architecture:
 * 1. Load conversation history from Redis (suspend/coroutine)
 * 2. Execute chat pipeline to process query (suspend/coroutine)
 * 3. Stream AI response as Flux<String> (reactive streaming)
 * 4. Save conversation history asynchronously (coroutine)
 */
@Service
class DocumentBaseChatService(
    private val chatClient: ChatClient,
    private val documentChatPipeline: DocumentChatPipeline,
    private val sessionManagementService: SessionManagementService,
    private val chatEventBus: ChatEventBus,
    private val toolCallbacks: List<ToolCallback>,
    @Autowired(required = false) private val mcpToolCallbackProvider: SyncMcpToolCallbackProvider?,
    @Value("\${spring.ai.openai.chat.options.model:gpt-4o-mini}") private val modelName: String,
    private val meterRegistry: MeterRegistry,
    private val tracer: Tracer,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val timeLimiterRegistry: TimeLimiterRegistry
) : ChatService {

    private val allToolCallbacks: List<ToolCallback> by lazy {
        val mcpTools = mcpToolCallbackProvider?.toolCallbacks?.toList() ?: emptyList()
        log.info { "Loaded ${toolCallbacks.size} regular tools and ${mcpTools.size} MCP tools" }
        mcpTools.map {
            log.info { "MCP Tool: ${it.toolDefinition.name()}" }
        }
        toolCallbacks + mcpTools
    }

    /**
     * Process user query and generate AI response with conversation history support
     */
    override suspend fun chat(
        chatServiceRequest: ChatServiceRequest
    ): Flux<String> {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        val requestId = MDC.get("requestId") ?: UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        val sample = Timer.start(meterRegistry) // Start timer
        val actualSessionId = generateSessionIdIfNotProvided(chatServiceRequest)

        val message = chatServiceRequest.message.trim()
        val keywords = chatServiceRequest.keywords
        val isDeepThink = chatServiceRequest.isDeepThink
        val conversationHistory = loadConversationHistory(actualSessionId)

        val parentSpan = tracer.spanBuilder("chat.process")
            .startSpan()
            .apply {
                setAttribute("chat.session_id", actualSessionId)
                setAttribute("chat.request_id", requestId)
                setAttribute("chat.model", modelName)
                setAttribute("chat.deep_think", isDeepThink)
                chatServiceRequest.userEmail?.let { setAttribute("chat.user_email", it) }
            }
        val parentScope = parentSpan.makeCurrent()

        val context = ChatContext(
            input = ChatContext.UserInput(
                message = message,
                providedKeywords = keywords,
                sessionId = actualSessionId,
                userEmail = chatServiceRequest.userEmail
            ),
            conversationHistory = conversationHistory,
            isDeepThink = isDeepThink
        )

        val processedContext = try {
            val pipelineSpan = tracer.spanBuilder("chat.pipeline").startSpan()
            try {
                documentChatPipeline.execute(context).also { pc ->
                    val queryType = pc.analysis?.queryAnalysis?.type?.name ?: "UNKNOWN"
                    val resultsCount = pc.search?.results?.size ?: 0
                    pipelineSpan.setAttribute("query.type", queryType)
                    pipelineSpan.setAttribute("search.results_count", resultsCount.toLong())
                    pipelineSpan.setAttribute("pipeline.steps", pc.executedStep.joinToString(" -> "))
                    meterRegistry.counter("chat.query.type.total", "type", queryType).increment()
                }
            } catch (e: Exception) {
                pipelineSpan.recordException(e)
                pipelineSpan.setStatus(StatusCode.ERROR, e.message ?: "pipeline failed")
                log.error(e) { "[Pipeline Error] Failed to execute pipeline" }
                meterRegistry.counter("chat.requests.total", "status", "failure", "model", modelName).increment()
                throw e
            } finally {
                pipelineSpan.end()
            }
        } catch (e: Exception) {
            parentSpan.recordException(e)
            parentSpan.setStatus(StatusCode.ERROR, e.message ?: "chat failed")
            parentScope.close()
            parentSpan.end()
            throw e
        }

        log.info { "[AI Processing] Starting AI chat with conversation context" }
        log.debug { "Context preview: ${processedContext.search?.contextText?.take(300)}..." }
        val prompt = generatePrompt(processedContext)
        val responseBuffer = StringBuffer()
        val toolCallbacks = if (!isDeepThink) {
            allToolCallbacks.filterNot { it.toolDefinition.name().startsWith("sequential-") }
        } else {
            allToolCallbacks
        }

        val aiSpan = tracer.spanBuilder("chat.ai_call").startSpan()
        val aiSpanEnded = AtomicBoolean(false)

        return try {
            chatClient
                .prompt(prompt)
                .toolCallbacks(toolCallbacks)
                .stream()
                .chatResponse() // Change to chatResponse() to access metadata
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("chat")))
                .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("chat")))
                .doOnNext { chatResponse ->
                    // Extract metadata (token usage)
                    val usage = chatResponse.metadata.usage
                    if (usage != null && (usage.totalTokens > 0 || usage.promptTokens > 0 || usage.completionTokens > 0)) {
                        if (usage.totalTokens > 0L) {
                            log.debug { "[Token Usage] Prompt: ${usage.promptTokens}, Generation: ${usage.completionTokens}, Total: ${usage.totalTokens}" }
                            meterRegistry.counter("ai.tokens.prompt", "model", modelName).increment(usage.promptTokens.toDouble())
                            meterRegistry.counter("ai.tokens.completion", "model", modelName).increment(usage.completionTokens.toDouble())
                            meterRegistry.counter("ai.tokens.total", "model", modelName).increment(usage.totalTokens.toDouble())
                            aiSpan.setAttribute("ai.prompt_tokens", usage.promptTokens.toLong())
                            aiSpan.setAttribute("ai.completion_tokens", usage.completionTokens.toLong())
                            aiSpan.setAttribute("ai.total_tokens", usage.totalTokens.toLong())
                        }
                    }
                }
                .map { it.results.firstOrNull()?.output?.text ?: "" } // Map back to content string, safe access
                .onErrorResume(CallNotPermittedException::class.java) { fallbackChat(it) }
                .onErrorResume(TimeoutException::class.java) { fallbackChat(it) }
                .normalizeMarkdownLines() // Post-process markdown syntax before sending to frontend
                .doOnNext { chunk ->
                    // Log chunks with visible newline indicators for debugging
                    val debugChunk = chunk.replace("\n", "\\n").replace("\r", "\\r")
                    if (debugChunk.contains("\\n")) {
                        log.debug { "[Chunk with newline] $debugChunk" }
                    }
                    responseBuffer.append(chunk)
                }
                .doOnComplete {
                    // Log final response to check if newlines are preserved
                    val finalResponse = responseBuffer.toString()
                    val newlineCount = finalResponse.count { it == '\n' }
                    val responseTime = System.currentTimeMillis() - startTime
                    log.info { "[Chat Completed] Total response length: ${finalResponse.length}, newlines: $newlineCount, time: ${responseTime}ms" }
                    if (newlineCount < 5) {
                        log.warn { "⚠️ WARNING: Response has few newlines ($newlineCount). Post-processing was applied." }
                    }

                    aiSpan.setAttribute("ai.response_length", finalResponse.length.toLong())
                    aiSpan.addEvent("ai.response.generated")

                    // Record metrics on completion
                    meterRegistry.counter("chat.requests.total", "status", "success", "model", modelName).increment()
                    sample.stop(
                        Timer.builder("chat.response.time")
                            .description("Time taken to generate chat response")
                            .tags(listOf(Tag.of("model", modelName)))
                            .register(meterRegistry)
                    )

                    parentSpan.setAttribute("chat.success", true)
                    parentSpan.setAttribute("chat.response_time_ms", responseTime)
                    parentSpan.addEvent("chat.completed")

                    chatEventBus.publish(
                        ConversationHistorySaveEvent(
                            sessionId = actualSessionId,
                            userMessage = message,
                            assistantResponse = finalResponse,
                            existingHistory = conversationHistory
                        )
                    )

                    chatEventBus.publish(
                        ChatInteractionCompletedEvent(
                            requestId = requestId,
                            sessionId = actualSessionId,
                            userMessage = message,
                            aiResponse = finalResponse,
                            responseTimeMs = responseTime,
                            processedContext = processedContext,
                            isDeepThink = isDeepThink,
                            userEmail = chatServiceRequest.userEmail,
                            modelUsed = modelName
                        )
                    )
                }
                .doOnError { error ->
                    log.error(error) { "[Chat Error] Error during streaming" }
                    aiSpan.recordException(error)
                    aiSpan.setStatus(StatusCode.ERROR, error.message ?: "ai failed")
                    parentSpan.recordException(error)
                    parentSpan.setStatus(StatusCode.ERROR, error.message ?: "chat failed")
                    meterRegistry.counter("chat.requests.total", "status", "failure", "model", modelName).increment()
                }
                .doFinally {
                    if (aiSpanEnded.compareAndSet(false, true)) {
                        aiSpan.end()
                    }
                    parentScope.close()
                    parentSpan.end()
                    log.info { "[Chat Completed] Response stream finished" }
                    log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                }
        } catch (e: Exception) {
            aiSpan.recordException(e)
            aiSpan.setStatus(StatusCode.ERROR, e.message ?: "ai failed")
            if (aiSpanEnded.compareAndSet(false, true)) {
                aiSpan.end()
            }
            parentScope.close()
            parentSpan.end()
            throw e
        }
    }

    /**
     * Fallback for AI chat stream failures
     */
    private fun fallbackChat(throwable: Throwable): Flux<String> {
        log.warn(throwable) { "[Fallback] Circuit breaker or timeout occurred" }
        return Flux.just("⚠️ 현재 사용량이 많아 답변을 생성할 수 없습니다. 잠시 후 다시 시도해주세요.")
    }

    /**
     * Generate session ID if not provided
     */
    private fun generateSessionIdIfNotProvided(chatServiceRequest: ChatServiceRequest): String {
        val sessionId = chatServiceRequest.sessionId
        val actualSessionId = sessionId ?: sessionManagementService.generateSessionId()
        log.info { "Processing chat request - SessionId: ${sessionId ?: "NEW"}" }
        log.info { "Using session: $actualSessionId" }
        return actualSessionId
    }

    /**
     * Load conversation history from Redis using coroutine
     */
    private suspend fun loadConversationHistory(actualSessionId: String): ChatContext.ConversationHistory? {
        return sessionManagementService.loadConversationHistory(actualSessionId)
    }

    /**
     * Generate prompt with conversation history
     */
    private fun generatePrompt(processedContext: CompleteChatContext): Prompt {
        val basePrompt = processedContext.prompt.text
        val conversationHistoryPrompt = processedContext.conversationHistory?.let {
            buildString {
                appendLine("이전 대화 내역:")
                appendLine("---")
                it.messages.forEach { message ->
                    val roleLabel = if (message.role == "user") "사용자" else "어시스턴트"
                    appendLine("[$roleLabel]: ${message.content}")
                }
                appendLine("---")
                appendLine()
            }
        }

        val toolPrompt = """
                부족한 정보는 TOOL을 사용하여 보완하세요.
        """.trimIndent()

        return Prompt(
            conversationHistoryPrompt + basePrompt + toolPrompt
        )
    }

    /**
     * Extension function to normalize markdown syntax in streaming response
     * Buffers chunks by line and applies markdown normalization to complete lines
     * Tracks previous line to remove blank lines that cause <p> tags in lists
     */
    private fun Flux<String>.normalizeMarkdownLines(): Flux<String> = Flux.create { sink ->
        val buffer = StringBuilder()
        var previousLine = ""

        this.subscribe(
            { chunk ->
                buffer.append(chunk)

                // Process and emit complete lines (ending with \n)
                var newlineIndex = buffer.indexOf('\n')
                while (newlineIndex != -1) {
                    val line = buffer.substring(0, newlineIndex + 1)
                    val normalized = normalizeMarkdown(line)

                    // Skip blank lines that appear after list items (prevents <p> tags in <li>)
                    val isBlankLine = normalized.trim().isEmpty()
                    val prevIsNumberedItem = previousLine.trim().matches(Regex("^\\d+\\.\\s+.*"))
                    val prevIsBulletItem = previousLine.trim().matches(Regex("^\\s*-\\s+.*"))

                    if (!(isBlankLine && (prevIsNumberedItem || prevIsBulletItem))) {
                        sink.next(normalized)
                    } else {
                        log.debug { "[Skipped blank line after list item] Previous: ${previousLine.trim()}" }
                    }

                    // Always update previousLine (even if we skip the current line)
                    if (!isBlankLine) {
                        previousLine = normalized
                    }

                    buffer.delete(0, newlineIndex + 1)
                    newlineIndex = buffer.indexOf('\n')
                }
            },
            { error -> sink.error(error) },
            {
                // Flush remaining buffer content (lines without final newline)
                if (buffer.isNotEmpty()) {
                    sink.next(normalizeMarkdown(buffer.toString()))
                }
                sink.complete()
            }
        )
    }

    /**
     * Normalize markdown syntax by adding missing spaces
     * Fixes common AI-generated markdown issues:
     * 1. Headings: ###Title -> ### Title
     * 2. Numbered lists: 1.**item** -> 1. **item**
     * 3. Bullet lists: -item -> - item (preserves indentation for nested lists)
     * 4. Unicode bullets: • item -> - item (converts unicode bullets to markdown)
     * 5. Nested list indentation: Ensures 4+ spaces for proper nesting
     * 6. Remove excessive blank lines to prevent <p> tags in lists
     */
    private fun normalizeMarkdown(text: String): String {
        var normalized = text

        // 0. Convert unicode bullet characters to markdown dashes
        // Common unicode bullets: • ● ○ ■ □ ▪ ▫ ‣ ⁃
        normalized = normalized.replace(Regex("^(\\s*)([•●○■□▪▫‣⁃])(\\s+)", setOf(RegexOption.MULTILINE)), "$1- ")
        normalized = normalized.replace(Regex("^(\\s*)([•●○■□▪▫‣⁃])(?!\\s)", setOf(RegexOption.MULTILINE)), "$1- ")

        // 1. Add space after heading symbols if missing (###Title -> ### Title)
        normalized = normalized.replace(Regex("^(#{1,6})([^\\s#])", setOf(RegexOption.MULTILINE)), "$1 $2")

        // 2. Add space after numbered list markers if missing (1.**item** -> 1. **item**)
        normalized = normalized.replace(Regex("^(\\d+\\.)(\\S)", setOf(RegexOption.MULTILINE)), "$1 $2")

        // 3. Fix nested list indentation: ensure at least 4 spaces for bullets under numbered lists
        // "  - item" (2-3 spaces) -> "    - item" (4 spaces) for proper markdown nesting
        normalized = normalized.replace(Regex("^(\\s{1,3})(-\\s)", setOf(RegexOption.MULTILINE)), "    $2")

        // 4. Fix indented bullet lists with 4+ spaces: just add space after dash if missing
        normalized = normalized.replace(Regex("^(\\s{4,})-(\\S)", setOf(RegexOption.MULTILINE)), "$1- $2")

        // 5. Add space after list markers at line start if missing (-item -> - item)
        normalized = normalized.replace(Regex("^-(\\S)", setOf(RegexOption.MULTILINE)), "- $1")

        // 6. Remove excessive blank lines to prevent excessive spacing
        // Limit consecutive newlines to maximum 2 (= 1 blank line)
        normalized = normalized.replace(Regex("\n{3,}"), "\n\n")

        // Remove blank lines between numbered list items to prevent <p> tags
        normalized = normalized.replace(Regex("\n\n+(\\d+\\.)"), "\n$1")
        // Remove blank lines between bullet items to prevent <p> tags (including indented bullets)
        normalized = normalized.replace(Regex("\n\n+(\\s*-)"), "\n$1")
        // Remove blank lines after bullet items when followed by indented content (prevents <p> in <li>)
        normalized = normalized.replace(Regex("\n\n+(\\s{2,}-)"), "\n$1")
        // Remove blank lines after indented bullets (critical for nested list items)
        normalized = normalized.replace(Regex("(\\s+-.+)\\n\\n+(\\s+-)"), "$1\n$2")

        // Clean up trailing spaces/tabs at end of lines (but preserve newlines!)
        normalized = normalized.replace(Regex("[ \\t]+$", setOf(RegexOption.MULTILINE)), "")

        return normalized
    }
}
