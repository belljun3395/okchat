package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeyWordExtractionPrompt
import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.model.PromptExample
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

/**
 * Extracts search-optimized keywords from user queries for hybrid search (BM25 + semantic).
 *
 * Purpose: Convert natural language queries into searchable keywords
 * Target: 7-10 keywords (max 12), ordered by importance
 * Output: Korean and English terms, technical concepts, entities
 *
 * Example: execute("백엔드 개발 레포 정보")
 *          → ["백엔드", "backend", "개발", "development", "레포", "repository"]
 */
@Service
class KeywordExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    override fun buildPrompt(message: String): Prompt {
        val instruction = """
Extract 7-10 core keywords from the user message for hybrid search (BM25 + semantic).
Order them from most important to least important.

EXTRACT (include both Korean and English):
- Core subjects and topics (the 'what', not the 'how')
- Technical terms and technologies
- Entities (people, teams, products, projects)

AVOID (these are handled separately or add noise):
- Common action verbs or requests (e.g., 알려줘, 찾아줘, 검색해, 요약해, search, find, summarize)
- Dates and numbers (handled by DateExtractor)
- Stop words (이, 그, 저, the, a, an, is, are)
- Morphological variations (개발/개발자/개발팀 - pick one)
- Overly generic terms alone ("문서", "정보", "내용")
- Ambiguous short forms (<2 characters)

TARGET: 7-10 keywords for broader coverage (max 12)
        """.trimIndent()

        val examples = listOf(
            PromptExample(
                input = "백엔드 개발 레포 정보",
                output = "백엔드, backend, 개발, development, 레포, repository"
            ),
            PromptExample(
                input = "PPP 개발 회의록 문의",
                output = "PPP, 개발, development, 회의록, 회의, meeting minutes"
            ),
            PromptExample(
                input = "2025년 9월 주간회의록 요약",
                output = "주간회의록, 회의록, 회의, weekly meeting, meeting minutes, meeting"
            ),
            PromptExample(
                input = "Show me the design document for the new authentication logic in the Mobile App project.",
                output = "design document, authentication logic, authentication, logic, Mobile App project, Mobile App"
            ),
            PromptExample(
                input = "개발팀 스페이스에 있는 지난 주 회의록 찾아줘",
                output = "개발팀 스페이스, 개발팀, 지난 주 회의록, 회의록, 회의"
            )
        )

        return KeyWordExtractionPrompt(
            userInstruction = instruction,
            examples = examples,
            message = message
        )
    }

    override fun getOptions(): OpenAiChatOptions {
        return OpenAiChatOptions.builder()
            .temperature(0.3) // Slightly higher for keyword variety
            .maxTokens(100)
            .build()
    }

    override fun getMinKeywordLength(): Int = 2

    override fun getMaxKeywords(): Int = 12

    override fun getEmptyResult(): List<String> = emptyList()

    override fun getFallbackMessage(): String = "Using fallback strategy."
}
