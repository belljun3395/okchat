package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for extracting content-related keywords from a user query using an LLM.
 */
@Service
class ContentExtractionService(
    private val chatModel: ChatModel
) {

    /**
     * Extracts keywords describing the main subject or topic of the content the user is looking for.
     */
    suspend fun extractContentKeywords(message: String): List<String> {
        val contentPrompt = """
            From the user's query, extract keywords that describe the main subject, topic, or key concepts of the content they are looking for.
            Order them from most important to least important.
            Focus on the 'what' of the query, not the action.

            - Extract nouns and technical terms that describe the subject matter.
            - Exclude action words (e.g., "explain", "tell me about"), greetings, and conversational filler.
            - Exclude file names or titles, focusing only on the conceptual topic.

            FORMAT: Comma-separated list, ordered from most important to least important.

            EXAMPLES:
            - User Query: "Tell me about the new authentication logic for the PPP project."
              Output: "new authentication logic, authentication, logic, PPP project"
            - User Query: "How is the memory leak in the notification service being handled?"
              Output: "memory leak, notification service"
            - User Query: "성능 테스트 결과 보고서에서 병목 현상에 대한 내용 찾아줘"
              Output: "병목 현상, 성능 테스트"
            - User Query: "I want to know the agenda for the next weekly meeting."
              Output: "weekly meeting, agenda"

            User query: "$message"

            Content Keywords (comma-separated, most important first):
        """.trimIndent()

        return try {
            val options = OpenAiChatOptions.builder()
                .temperature(0.2)
                .maxTokens(100)
                .build()

            val response = chatModel.call(Prompt(contentPrompt, options))
            val keywordsText = response.result.output.text?.trim()
            log.debug { "Extracted content keywords: $keywordsText" }

            if (keywordsText.isNullOrBlank()) {
                emptyList()
            } else {
                keywordsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
            }
        } catch (e: Exception) {
            log.warn { "Failed to extract content keywords: ${e.message}. Returning empty list." }
            emptyList()
        }
    }
}
