package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for extracting keywords from text using LLM
 * Supports both Korean and English keyword extraction
 */
@Service
class KeywordExtractionService(
    private val chatModel: ChatModel
) {

    /**
     * Extract keywords from the message using LLM (in both Korean and English)
     */
    suspend fun extractKeywords(message: String): List<String> {
        val keywordPrompt = """
            Extract important keywords from the following user message for document search.

            IMPORTANT: Provide keywords in BOTH Korean and English when applicable.
            - If the message is in Korean, provide both Korean terms and their English equivalents
            - If the message is in English, provide both English terms and their Korean equivalents
            - For technical terms, include both languages (e.g., "백엔드, backend, 개발, development")

            Return ONLY the keywords separated by commas, without any explanation or formatting.

            Examples:
            - Input: "백엔드 개발 레포 정보"
              Output: "백엔드, backend, 개발, development, 레포, repository, 정보, information"

            - Input: "User Guide folder contents"
              Output: "User Guide, 유저 가이드, folder, 폴더, contents, 내용"

            User message: "$message"

            Keywords (Korean and English):
        """.trimIndent()

        return try {
            val response = chatModel.call(Prompt(keywordPrompt))
            val keywordsText = response.result.output.text?.trim()
            log.debug { "Extracted keywords (KR+EN): $keywordsText" }

            val keywords = keywordsText?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf(message)

            // Remove duplicates (case-insensitive)
            keywords.distinctBy { it.lowercase() }
        } catch (e: Exception) {
            log.warn { "Failed to extract keywords: ${e.message}. Using original message." }
            listOf(message)
        }
    }

    /**
     * Extract keywords from document content for indexing
     * This method is optimized for document content rather than user queries
     */
    suspend fun extractKeywordsFromContent(content: String, title: String? = null): List<String> {
        val contentPrompt = """
            Extract important keywords from the following document content for search indexing.

            IMPORTANT: Provide keywords in BOTH Korean and English when applicable.
            - Focus on key concepts, technologies, processes, and topics
            - Include both Korean terms and their English equivalents
            - For technical terms, include both languages
            - Prioritize terms that would be useful for document search

            Return ONLY the keywords separated by commas, without any explanation or formatting.

            Document Title: ${title ?: "N/A"}
            Document Content: ${content.take(2000)}...

            Keywords (Korean and English):
        """.trimIndent()

        return try {
            val response = chatModel.call(Prompt(contentPrompt))
            val keywordsText = response.result.output.text?.trim()
            log.debug { "Extracted keywords from content: $keywordsText" }

            val keywords = keywordsText?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            // Remove duplicates (case-insensitive)
            keywords.distinctBy { it.lowercase() }
        } catch (e: Exception) {
            log.warn { "Failed to extract keywords from content: ${e.message}. Returning empty list." }
            emptyList()
        }
    }
}
