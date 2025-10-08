package com.okestro.okchat.ai.support.extraction

import com.okestro.okchat.ai.support.PromptTemplate
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Service for extracting title-related keywords from a user query using an LLM.
 * Extends BaseExtractionService to eliminate code duplication.
 */
@Service
class TitleExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    /**
     * Extracts keywords from the user's message that are likely to be part of a document title.
     */
    suspend fun extractTitleKeywords(message: String): List<String> {
        return execute(message)
    }

    override fun buildPrompt(message: String): String {
        val instruction = """
Extract only the words that seem to be part of a specific document title.
Order them from most important to least important.
If no specific title is mentioned, return an empty string.

- Focus on proper nouns, report names, or quoted text.
- Exclude generic terms like "document", "file", "report" unless they are part of a specific name.
- Exclude action words (e.g., "find", "show me", "summarize").
        """.trimIndent()

        val examples = PromptTemplate.formatExamples(
            "show me the 'Q3 Performance Review' document" to "Q3 Performance Review",
            "2025년 기술 부채 보고서 찾아줘" to "2025년 기술 부채 보고서, 기술 부채 보고서",
            "회의록 좀 찾아줄래?" to "회의록",
            "How does the login logic work?" to ""
        )

        return PromptTemplate.buildExtractionPrompt(instruction, examples, message)
    }
}
