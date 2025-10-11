package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeyWordExtractionPrompt
import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.model.PromptExample
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Extracts content-related keywords describing the main subject or topic of a query.
 *
 * Purpose: Extract keywords about 'what' content the user is looking for, not 'how'
 * Focus: Nouns, technical terms, subject matter
 * Output: Main topics, concepts, domain terminology
 *
 * Example: execute("Tell me about authentication logic")
 *          → ["authentication logic", "authentication", "logic"]
 */
@Service
class ContentExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    override fun buildPrompt(message: String): Prompt {
        val instruction = """
Extract keywords that describe the main subject, topic, or key concepts of the content.
Order them from most important to least important.
Focus on the 'what' of the query, not the action.

- Extract nouns and technical terms that describe the subject matter.
- Exclude action words (e.g., "explain", "tell me about"), greetings, and conversational filler.
- Exclude file names or titles, focusing only on the conceptual topic.
        """.trimIndent()

        val examples = listOf(
            PromptExample(
                input = "Tell me about the new authentication logic for the PPP project.",
                output = "new authentication logic, authentication, logic, PPP project"
            ),
            PromptExample(
                input = "How is the memory leak in the notification service being handled?",
                output = "memory leak, notification service"
            ),
            PromptExample(
                input = "성능 테스트 결과 보고서에서 병목 현상에 대한 내용 찾아줘",
                output = "병목 현상, 성능 테스트"
            ),
            PromptExample(
                input = "I want to know the agenda for the next weekly meeting.",
                output = "weekly meeting, agenda"
            )
        )

        return KeyWordExtractionPrompt(
            instruction = instruction,
            examples = examples,
            message = message
        )
    }
}
