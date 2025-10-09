package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.CompleteChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

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
    private val toolCallbacks: List<ToolCallback>,
    @Autowired(required = false) private val mcpToolCallbackProvider: SyncMcpToolCallbackProvider?
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
        val actualSessionId = generateSessionIdIfNotProvided(chatServiceRequest)

        val message = chatServiceRequest.message.trim()
        val keywords = chatServiceRequest.keywords
        val isDeepThink = chatServiceRequest.isDeepThink
        val conversationHistory = loadConversationHistory(actualSessionId)
        val context = ChatContext(
            input = ChatContext.UserInput(
                message = message,
                providedKeywords = keywords,
                sessionId = actualSessionId
            ),
            conversationHistory = conversationHistory,
            isDeepThink = isDeepThink
        )

        val processedContext = documentChatPipeline.execute(context)

        log.info { "[AI Processing] Starting AI chat with conversation context" }
        log.debug { "Context preview: ${processedContext.search?.contextText?.take(300)}..." }
        val prompt = generatePrompt(processedContext)
        val responseBuffer = StringBuffer()
        val toolCallbacks = if (!isDeepThink) {
            allToolCallbacks.filterNot { it.toolDefinition.name().startsWith("sequential-") }
        } else {
            allToolCallbacks
        }

        return chatClient.prompt(prompt)
            .toolCallbacks(toolCallbacks)
            .stream()
            .content() // Returns Flux<String> - streaming response!
            .doOnNext { chunk ->
                responseBuffer.append(chunk)
                // Log chunks with visible newline indicators for debugging
                val debugChunk = chunk.replace("\n", "\\n").replace("\r", "\\r")
                if (debugChunk.contains("\\n")) {
                    log.debug { "[Chunk with newline] $debugChunk" }
                }
            }
            .doOnComplete {
                // Log final response to check if newlines are preserved
                val finalResponse = responseBuffer.toString()
                val newlineCount = finalResponse.count { it == '\n' }
                log.info { "[Chat Completed] Total response length: ${finalResponse.length}, newlines: $newlineCount" }
                if (newlineCount < 5) {
                    log.warn { "⚠️ WARNING: Response has few newlines ($newlineCount). Post-processing was applied." }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    saveConversationHistory(responseBuffer, actualSessionId, message, conversationHistory)
                }
            }
            .doFinally {
                log.info { "[Chat Completed] Response stream finished" }
                log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
            }
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
     * save conversation history with error handling
     */
    private suspend fun saveConversationHistory(
        responseBuffer: StringBuffer,
        actualSessionId: String,
        message: String,
        conversationHistory: ChatContext.ConversationHistory?
    ) {
        try {
            val assistantResponse = responseBuffer.toString()
            sessionManagementService.saveConversationHistory(
                sessionId = actualSessionId,
                userMessage = message,
                assistantResponse = assistantResponse,
                existingHistory = conversationHistory
            )
            log.info { "[Session Saved] Conversation history updated for session: $actualSessionId" }
        } catch (e: Exception) {
            log.error(e) { "[Session Save Error] Failed to save conversation history" }
        }
    }
}
