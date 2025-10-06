package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.ChatPipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

private val log = KotlinLogging.logger {}

/**
 * Main chat service for handling user queries
 * Uses a pipeline-based architecture for extensibility
 */
@Service
class DocumentBaseChatService(
    private val chatClient: ChatClient,
    private val chatPipeline: ChatPipeline,
    private val toolCallbacks: List<ToolCallback>
) {

    /**
     * Process user query and generate AI response
     */
    fun chat(message: String, keywords: List<String>?): Flux<String> {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        return mono {
            // Create initial context with user input
            val initialContext = ChatContext(
                input = ChatContext.UserInput(
                    message = message,
                    providedKeywords = keywords ?: emptyList()
                )
            )

            // Execute pipeline to process query and prepare prompt
            val processedContext = chatPipeline.execute(initialContext)

            log.info { "[AI Processing] Starting AI chat" }
            log.debug { "Context preview: ${processedContext.search?.contextText?.take(300)}..." }

            // Get fully rendered prompt text (already processed by PromptGenerationStep)
            val promptText = processedContext.prompt.text

            // Use the rendered prompt directly (no need for PromptTemplate again)
            Prompt(
                promptText + """
                부족한 정보는 TOOL을 사용하여 보완하세요.
                """.trimIndent()
            )
        }
            .flatMapMany { prompt ->
                chatClient.prompt(prompt)
                    .toolCallbacks(toolCallbacks)
                    .stream()
                    .content()
                    .doOnComplete {
                        log.info { "[Chat Completed] Response stream finished" }
                        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                    }
                    .doOnError { error ->
                        log.error(error) { "[Chat Error] ${error.message}" }
                        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                    }
            }
    }
}
