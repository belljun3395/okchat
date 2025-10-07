package com.okestro.okchat.ai.support

import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Service for extracting content-related keywords from a user query using an LLM.
 * Extends BaseExtractionService to eliminate code duplication.
 */
@Service
class ContentExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    /**
     * Extracts keywords describing the main subject or topic of the content the user is looking for.
     */
    suspend fun extractContentKeywords(message: String): List<String> {
        return execute(message)
    }

    override fun buildPrompt(message: String): String {
        val instruction = """
Extract keywords that describe the main subject, topic, or key concepts of the content.
Order them from most important to least important.
Focus on the 'what' of the query, not the action.

- Extract nouns and technical terms that describe the subject matter.
- Exclude action words (e.g., "explain", "tell me about"), greetings, and conversational filler.
- Exclude file names or titles, focusing only on the conceptual topic.
        """.trimIndent()

        val examples = PromptTemplate.formatExamples(
            "Tell me about the new authentication logic for the PPP project." to "new authentication logic, authentication, logic, PPP project",
            "How is the memory leak in the notification service being handled?" to "memory leak, notification service",
            "성능 테스트 결과 보고서에서 병목 현상에 대한 내용 찾아줘" to "병목 현상, 성능 테스트",
            "I want to know the agenda for the next weekly meeting." to "weekly meeting, agenda"
        )

        return PromptTemplate.buildExtractionPrompt(instruction, examples, message)
    }
}
