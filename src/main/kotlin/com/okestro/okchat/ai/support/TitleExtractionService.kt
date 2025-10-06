package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for extracting title-related keywords from a user query using an LLM.
 */
@Service
class TitleExtractionService(
    private val chatModel: ChatModel
) {

    /**
     * Extracts keywords from the user's message that are likely to be part of a document title.
     */
    suspend fun extractTitleKeywords(message: String): List<String> {
        val titlePrompt = """
            From the user's query, extract only the words that seem to be part of a specific document title.
            Order them from most important to least important.
            If no specific title is mentioned, return an empty string.

            - Focus on proper nouns, report names, or quoted text.
            - Exclude generic terms like "document", "file", "report" unless they are part of a specific name.
            - Exclude action words (e.g., "find", "show me", "summarize").

            FORMAT: Comma-separated list, ordered from most important to least important.

            EXAMPLES:
            - User Query: "show me the 'Q3 Performance Review' document"
              Output: "Q3 Performance Review"
            - User Query: "2025년 기술 부채 보고서 찾아줘"
              Output: "2025년 기술 부채 보고서, 기술 부채 보고서"
            - User Query: "회의록 좀 찾아줄래?"
              Output: "회의록"
            - User Query: "How does the login logic work?"
              Output: ""

            User query: "$message"

            Title Keywords (comma-separated, most important first):
        """.trimIndent()

        return try {
            val options = OpenAiChatOptions.builder()
                .temperature(0.2)
                .maxTokens(100)
                .build()

            val response = chatModel.call(Prompt(titlePrompt, options))
            val keywordsText = response.result.output.text?.trim()
            log.debug { "Extracted title keywords: $keywordsText" }

            if (keywordsText.isNullOrBlank()) {
                emptyList()
            } else {
                keywordsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
            }
        } catch (e: Exception) {
            log.warn { "Failed to extract title keywords: ${e.message}. Returning empty list." }
            emptyList()
        }
    }
}
