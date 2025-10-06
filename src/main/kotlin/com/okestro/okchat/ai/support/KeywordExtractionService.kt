package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
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
     * Optimized for RRF-based hybrid search: quality over quantity
     * Ordered from most to least important
     */
    suspend fun extractKeywords(message: String): List<String> {
        val keywordPrompt = """
            Extract 5-8 core keywords from the user message for hybrid search (BM25 + semantic).
            And Order them from most important to least important.

            EXTRACT (include both Korean and English):
            ✅ Core subjects and topics (the 'what', not the 'how')
            ✅ Technical terms and technologies
            ✅ Entities (people, teams, products, projects)
            ✅ For compound terms: full term + meaningful components
               Example: "주간회의록" → "주간회의록, 회의록, 회의" (broader search)
               Example: "개발파트" → "개발파트, 개발" (broader search)

            AVOID (these are handled separately or add noise):
            ❌ Common action verbs or requests (e.g., 알려줘, 찾아줘, 검색해, 요약해, search, find, summarize)
            ❌ Dates and numbers (handled by DateExtractor)
            ❌ Stop words (이, 그, 저, the, a, an, is, are)
            ❌ Morphological variations (개발/개발자/개발팀 - pick one)
            ❌ Overly generic terms alone ("문서", "정보", "내용")
            ❌ Ambiguous short forms (<2 characters)

            FORMAT: Comma-separated list only, no explanation
            TARGET: 7-10 keywords for broader coverage (max 12)

            EXAMPLES:
            ✅ Input: "백엔드 개발 레포 정보"
               Output: "백엔드, backend, 개발, development, 레포, repository"
               (6 keywords - good balance, skipped generic "정보")

            ✅ Input: "PPP 개발 회의록 문의"
               Output: "PPP, 개발, development, 회의록, 회의, meeting minutes"
               (6 keywords - '문의' is an action/request, so it's excluded)

            ❌ Input: "2025년 9월 주간회의록 요약"
               Bad: "2025, 2025년, 9월, September, 09, 250, 주간, 회의록, 회의, 주간회의, meeting, weekly, minutes"
               (13 keywords - too many, includes dates, over-variations)
               Good: "주간회의록, 회의록, 회의, weekly meeting, meeting minutes, meeting"
               (6 keywords - '요약' is an action, excluded. Dates handled separately)

            User message: "$message"

            Keywords (7-10 core terms):
        """.trimIndent()

        return try {
            // Use lower temperature for consistent keyword extraction
            val options = OpenAiChatOptions.builder()
                .temperature(0.3) // Lower = more consistent (vs 0.7 default)
                .maxTokens(100) // Limit output length for efficiency
                .build()

            val response = chatModel.call(Prompt(keywordPrompt, options))
            val keywordsText = response.result.output.text?.trim()
            log.debug { "Extracted keywords (KR+EN): $keywordsText" }

            val keywords = keywordsText?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.filter { it.length >= 2 } // Remove single characters or too short
                ?: listOf(message)

            // Remove duplicates (case-insensitive) and limit to 12 max (RRF optimization)
            keywords.distinctBy { it.lowercase() }.take(12)
        } catch (e: Exception) {
            log.warn { "Failed to extract keywords: ${e.message}. Using original message." }
            listOf(message)
        }
    }

    /**
     * Extract keywords from document content for indexing
     * This method is optimized for document content rather than user queries
     */
    suspend fun extractKeywordsFromContentAndTitle(content: String, title: String? = null): List<String> {
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
