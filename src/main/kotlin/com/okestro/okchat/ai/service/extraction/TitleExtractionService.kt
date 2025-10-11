package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeyWordExtractionPrompt
import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.model.PromptExample
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Extracts title-related keywords from queries mentioning specific document titles.
 *
 * Purpose: Identify explicit document title mentions in user queries
 * Focus: Proper nouns, report names, quoted text
 * Output: Document titles, report names (empty if no title mentioned)
 *
 * Example: execute("show me the 'Q3 Performance Review' document")
 *          → ["Q3 Performance Review"]
 */
@Service
class TitleExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    override fun buildPrompt(message: String): Prompt {
        val instruction = """
Extract only the words that seem to be part of a specific document title.
Order them from most important to least important.
If no specific title is mentioned, return an empty string.

- Focus on proper nouns, report names, or quoted text.
- Exclude generic terms like "document", "file", "report" unless they are part of a specific name.
- Exclude action words (e.g., "find", "show me", "summarize").
        """.trimIndent()

        val examples = listOf(
            PromptExample(
                input = "show me the 'Q3 Performance Review' document",
                output = "Q3 Performance Review"
            ),
            PromptExample(
                input = "2025년 기술 부채 보고서 찾아줘",
                output = "2025년 기술 부채 보고서, 기술 부채 보고서"
            ),
            PromptExample(
                input = "회의록 좀 찾아줄래?",
                output = "회의록"
            ),
            PromptExample(
                input = "How does the login logic work?",
                output = ""
            )
        )

        return KeyWordExtractionPrompt(
            instruction = instruction,
            examples = examples,
            message = message
        )
    }
}
