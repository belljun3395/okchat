package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeywordResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import com.okestro.okchat.ai.model.Prompt as OkChatPrompt

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
    protected val chatModel: ChatModel
) {
    protected val log = KotlinLogging.logger {}
    protected val outputConverter = BeanOutputConverter(KeywordResult::class.java)

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
    protected abstract fun buildPrompt(message: String): OkChatPrompt

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
     * Uses BeanOutputConverter for structured JSON parsing.
     * Falls back to simple comma-separated splitting.
     */
    /**
     * Parse the LLM result text into a list of keywords.
     * Uses BeanOutputConverter for structured JSON parsing.
     * Falls back to simple comma-separated splitting.
     * Applies post-processing: distinct, min length, max count.
     */
    private fun parseResult(resultText: String?): List<String> {
        val rawKeywords = try {
            if (resultText.isNullOrBlank()) return emptyList()
            outputConverter.convert(resultText)?.keywords ?: emptyList()
        } catch (e: Exception) {
            log.warn { "Failed to parse structured output: ${e.message}. Fallback to manual parsing." }
            resultText?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        }

        return rawKeywords
            .filter { it.length >= getMinKeywordLength() }
            .distinctBy { it.lowercase() }
            .take(getMaxKeywords())
    }

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

    /**
     * Get minimum length for a keyword to be valid.
     * Default: 2 characters.
     */
    protected open fun getMinKeywordLength(): Int = 2

    /**
     * Get maximum number of keywords to return.
     * Default: 20 keywords.
     */
    protected open fun getMaxKeywords(): Int = 20
}
