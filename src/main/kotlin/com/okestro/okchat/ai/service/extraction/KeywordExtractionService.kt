package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.support.PromptTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for extracting keywords from text using LLM.
 * Supports both Korean and English keyword extraction.
 * Extends BaseExtractionService but with custom settings for query extraction.
 */
@Service
class KeywordExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    /**
     * Extract keywords from the message using LLM (in both Korean and English)
     * Optimized for RRF-based hybrid search: quality over quantity
     * Ordered from most to least important
     *
     * Note: This is a public wrapper around the protected base method
     */
    suspend fun extractQueryKeywords(message: String): List<String> {
        return execute(message)
    }

    override fun buildPrompt(message: String): String {
        val instruction = """
Extract 5-8 core keywords from the user message for hybrid search (BM25 + semantic).
Order them from most important to least important.

EXTRACT (include both Korean and English):
- Core subjects and topics (the 'what', not the 'how')
- Technical terms and technologies
- Entities (people, teams, products, projects)
- For compound terms: full term + meaningful components
   Example: "주간회의록" → "주간회의록, 회의록, 회의" (broader search)
   Example: "개발파트" → "개발파트, 개발" (broader search)

AVOID (these are handled separately or add noise):
- Common action verbs or requests (e.g., 알려줘, 찾아줘, 검색해, 요약해, search, find, summarize)
- Dates and numbers (handled by DateExtractor)
- Stop words (이, 그, 저, the, a, an, is, are)
- Morphological variations (개발/개발자/개발팀 - pick one)
- Overly generic terms alone ("문서", "정보", "내용")
- Ambiguous short forms (<2 characters)

TARGET: 7-10 keywords for broader coverage (max 12)
        """.trimIndent()

        val examples = """
EXAMPLES:
- Input: "백엔드 개발 레포 정보"
  Good: "백엔드, backend, 개발, development, 레포, repository"
  (6 keywords - good balance, skipped generic "정보")

- Input: "PPP 개발 회의록 문의"
  Good: "PPP, 개발, development, 회의록, 회의, meeting minutes"
  (6 keywords - '문의' is an action/request, so it's excluded)

- Input: "2025년 9월 주간회의록 요약"
  Bad: "2025, 2025년, 9월, September, 09, 250, 주간, 회의록, 회의, 주간회의, meeting, weekly, minutes"
  (13 keywords - too many, includes dates, over-variations)
  Good: "주간회의록, 회의록, 회의, weekly meeting, meeting minutes, meeting"
  (6 keywords - '요약' is an action, excluded. Dates handled separately)
        """.trimIndent()

        return PromptTemplate.buildExtractionPrompt(instruction, examples, message)
    }

    override fun getOptions(): OpenAiChatOptions {
        return OpenAiChatOptions.builder()
            .temperature(0.3) // Slightly higher for keyword variety
            .maxTokens(100)
            .build()
    }

    override fun getMinKeywordLength(): Int = 2

    override fun getMaxKeywords(): Int = 12

    override fun getEmptyResult(): List<String> {
        // Return original message as fallback for query keywords
        return emptyList()
    }

    override fun getFallbackMessage(): String = "Using fallback strategy."

    /**
     * Extract keywords from document content for indexing.
     * This method is optimized for document content rather than user queries.
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
