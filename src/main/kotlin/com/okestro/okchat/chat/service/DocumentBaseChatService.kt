package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private val log = KotlinLogging.logger {}

/**
 * Main chat service for handling user queries
 * Uses a pipeline-based architecture for extensibility
 * Supports multi-turn conversations with session management
 */
@Service
class DocumentBaseChatService(
    private val chatClient: ChatClient,
    private val documentChatPipeline: DocumentChatPipeline,
    private val sessionManagementService: SessionManagementService,
    private val toolCallbacks: List<ToolCallback>,
    @Autowired(required = false)
    private val mcpToolCallbackProvider: SyncMcpToolCallbackProvider?
) {

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
    fun chat(message: String, keywords: List<String>? = null, sessionId: String? = null): Flux<String> {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        log.info { "Processing chat request - SessionId: ${sessionId ?: "NEW"}" }

        // Generate new session ID if not provided
        val actualSessionId = sessionId ?: sessionManagementService.generateSessionId()
        log.info { "Using session: $actualSessionId" }

        // Load conversation history if session exists
        val conversationHistory = if (sessionId != null) {
            sessionManagementService.loadConversationHistory(sessionId)
        } else {
            Mono.empty()
        }

        return conversationHistory
            .flatMapMany { conversationHistory ->
                processWithHistory(message, keywords, actualSessionId, conversationHistory)
            }
            .switchIfEmpty(
                processWithHistory(message, keywords, actualSessionId)
            )
    }

    /**
     * Process chat request with or without conversation history
     */
    private fun processWithHistory(
        message: String,
        keywords: List<String>?,
        actualSessionId: String,
        conversationHistory: ChatContext.ConversationHistory? = null
    ): Flux<String> {
        // Create initial context with user input and conversation history
        val initialContext = ChatContext(
            input = ChatContext.UserInput(
                message = message,
                providedKeywords = keywords ?: emptyList(),
                sessionId = actualSessionId
            ),
            conversationHistory = conversationHistory
        )

        // Log conversation history
        if (conversationHistory != null) {
            log.info { "Loaded conversation history: ${conversationHistory.messages.size} messages" }
            conversationHistory.messages.forEach { msg ->
                log.debug { "[${msg.role}] ${msg.content.take(100)}..." }
            }
        } else {
            log.info { "Starting new conversation session: $actualSessionId" }
        }

        return mono {
            // Execute pipeline to process query and prepare prompt
            val processedContext = documentChatPipeline.execute(initialContext)

            log.info { "[AI Processing] Starting AI chat with conversation context" }
            log.debug { "Context preview: ${processedContext.search?.contextText?.take(300)}..." }

            // Get fully rendered prompt text (already processed by PromptGenerationStep)
            val promptText = buildPromptWithHistory(
                basePrompt = processedContext.prompt.text,
                conversationHistory = conversationHistory
            )

            // Use the rendered prompt directly
            Prompt(
                promptText + """
                부족한 정보는 TOOL을 사용하여 보완하세요.
                """.trimIndent()
            )
        }
            .flatMapMany { prompt ->
                val responseBuffer = StringBuilder()

                chatClient.prompt(prompt)
                    .toolCallbacks(allToolCallbacks)
                    .stream()
                    .content()
                    .doOnNext { chunk ->
                        responseBuffer.append(chunk)
                    }
                    .publishOn(Schedulers.boundedElastic())
                    .doOnComplete {
                        // Save conversation history after response is complete
                        val assistantResponse = responseBuffer.toString()
                        sessionManagementService.saveConversationHistory(
                            sessionId = actualSessionId,
                            userMessage = message,
                            assistantResponse = assistantResponse,
                            existingHistory = conversationHistory
                        ).subscribe(
                            {
                                log.info { "[Session Saved] Conversation history updated for session: $actualSessionId" }
                            },
                            { error ->
                                log.error(error) { "[Session Save Error] Failed to save conversation history" }
                            }
                        )

                        log.info { "[Chat Completed] Response stream finished" }
                        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                    }
                    .doOnError { error ->
                        log.error(error) { "[Chat Error] ${error.message}" }
                        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                    }
            }
    }

    /**
     * Build prompt with conversation history context
     */
    private fun buildPromptWithHistory(
        basePrompt: String,
        conversationHistory: ChatContext.ConversationHistory?
    ): String {
        if (conversationHistory == null || conversationHistory.messages.isEmpty()) {
            return basePrompt
        }

        val historyContext = buildString {
            appendLine("이전 대화 내역:")
            appendLine("---")
            conversationHistory.messages.forEach { message ->
                val roleLabel = if (message.role == "user") "사용자" else "어시스턴트"
                appendLine("[$roleLabel]: ${message.content}")
            }
            appendLine("---")
            appendLine()
        }

        return historyContext + basePrompt
    }
}
