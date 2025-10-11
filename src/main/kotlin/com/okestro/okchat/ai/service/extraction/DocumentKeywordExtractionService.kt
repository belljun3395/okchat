package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeyWordExtractionPrompt
import com.okestro.okchat.ai.model.Prompt
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

/**
 * Extracts indexing-optimized keywords from document content for search.
 *
 * Purpose: Extract comprehensive keywords from documents for indexing
 * Target: 15-20 keywords (max 20), ordered by relevance
 * Output: Korean and English terms, technical concepts, domain terminology
 *
 * Example: extractFromDocument("Spring Boot application...", "API Documentation")
 *          → ["Spring Boot", "API", "REST", "authentication", ...]
 */
@Service
class DocumentKeywordExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    override fun buildPrompt(message: String): Prompt {
        val instruction = """
Extract 15-20 keywords from the document for comprehensive indexing.
Order from most to least relevant.

EXTRACT (include both Korean and English):
- Core concepts, methodologies, and topics
- Technical terms, tools, and technologies
- Domain-specific terminology
- Key processes and workflows
- Product names and entities

AVOID:
- Stop words and articles (the, a, an, 이, 그, 저)
- Navigation text or UI elements
- Overly generic terms without context
- Repeated morphological variations (use base form)

TARGET: 15-20 keywords for comprehensive document coverage (max 20)
        """.trimIndent()

        return KeyWordExtractionPrompt(
            userInstruction = instruction,
            examples = emptyList(), // No examples needed for document extraction
            message = message
        )
    }

    override fun getOptions(): OpenAiChatOptions {
        return OpenAiChatOptions.builder()
            .temperature(0.2) // Lower temperature for consistent document indexing
            .maxTokens(150) // More tokens for document keywords
            .build()
    }

    override fun getMinKeywordLength(): Int = 2

    override fun getMaxKeywords(): Int = 20 // More keywords for documents
}
