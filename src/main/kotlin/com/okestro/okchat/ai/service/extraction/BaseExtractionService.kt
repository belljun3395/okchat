package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.support.DefaultExtractionResultParser
import com.okestro.okchat.ai.support.ExtractionResultParser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions

/**
 * Base service for LLM-based keyword extraction.
 * Implements Template Method pattern to eliminate code duplication across extraction services.
 *
 * Common responsibilities:
 * - LLM interaction with consistent options
 * - Result parsing (comma-separated with fallbacks)
 * - Error handling
 * - Deduplication
 *
 * Subclass responsibilities:
 * - Define extraction prompt via buildPrompt()
 * - Optionally customize options via getOptions()
 * - Optionally customize empty result via getEmptyResult()
 */
abstract class BaseExtractionService(
    protected val chatModel: ChatModel,
    private val extractionResultParser: ExtractionResultParser = DefaultExtractionResultParser()
) {
    protected val log = KotlinLogging.logger {}

    /**
     * Extract keywords from message using LLM.
     * Template method that orchestrates the extraction process.
     * Uses suspend for future async ChatModel compatibility.
     */
    suspend fun execute(message: String): List<String> {
        val prompt = buildPrompt(message).toString()
        val options = getOptions()

        return try {
            val response = chatModel.call(Prompt(prompt, options))
            val resultText = response.result.output.text?.trim()

            log.debug { "[${this::class.simpleName}] Extracted: $resultText" }

            parseResult(resultText)
        } catch (e: Exception) {
            log.warn { "[${this::class.simpleName}] Failed to extract: ${e.message}. ${getFallbackMessage()}" }
            getEmptyResult()
        }
    }

    /**
     * Build the extraction prompt for the specific type.
     * Must be implemented by subclasses.
     */
    protected abstract fun buildPrompt(message: String): com.okestro.okchat.ai.model.Prompt

    /**
     * Get LLM options for extraction.
     * Override to customize (default: temperature=0.2, maxTokens=100)
     */
    protected open fun getOptions(): OpenAiChatOptions {
        return OpenAiChatOptions.builder()
            .temperature(0.2)
            .maxTokens(100)
            .build()
    }

    /**
     * Parse the LLM result text into a list of keywords.
     * Delegates to ResultParser for testability and flexibility.
     * Override to customize parsing logic.
     */
    protected open fun parseResult(resultText: String?): List<String> {
        return extractionResultParser.parse(
            resultText = resultText,
            minLength = getMinKeywordLength(),
            maxKeywords = getMaxKeywords(),
            emptyResult = getEmptyResult()
        )
    }

    /**
     * Get minimum keyword length (default: 1, can be overridden)
     */
    protected open fun getMinKeywordLength(): Int = 1

    /**
     * Get maximum number of keywords to return (default: unlimited, can be overridden)
     */
    protected open fun getMaxKeywords(): Int = Int.MAX_VALUE

    /**
     * Get result when extraction fails or returns empty.
     * Override to customize (default: empty list)
     */
    protected open fun getEmptyResult(): List<String> = emptyList()

    /**
     * Get fallback message for logging when extraction fails.
     * Override to customize.
     */
    protected open fun getFallbackMessage(): String = "Returning empty list."
}
