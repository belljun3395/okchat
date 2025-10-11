package com.okestro.okchat.ai.service.extraction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions

private val log = KotlinLogging.logger {}

/**
 * Base service for LLM-based keyword extraction.
 * Implements Template Method pattern to eliminate code duplication across extraction services.
 *
 * Common responsibilities:
 * - LLM interaction with consistent options
 * - Result parsing (comma-separated)
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

    /**
     * Extract keywords from message using LLM.
     * Template method that orchestrates the extraction process.
     */
    fun execute(message: String): List<String> {
        val prompt = buildPrompt(message)
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
    protected abstract fun buildPrompt(message: String): String

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
     * * Supports multiple formats for robustness:
     * 1. Comma-separated: "keyword1, keyword2, keyword3"
     * 2. Newline-separated: "keyword1\nkeyword2\nkeyword3"
     * 3. Numbered list: "1. keyword1\n2. keyword2"
     * 4. Bulleted list: "- keyword1\n- keyword2"
     * * Override to customize parsing logic.
     */
    protected open fun parseResult(resultText: String?): List<String> {
        if (resultText.isNullOrBlank()) {
            return getEmptyResult()
        }

        // Parse using multiple strategies
        val keywords = when {
            // Strategy 1: Comma-separated (most common from our prompts)
            resultText.contains(",") -> {
                resultText.split(",")
                    .map { it.trim() }
            }

            // Strategy 2: Newline-separated
            resultText.contains("\n") -> {
                resultText.lines()
                    .map { line ->
                        // Remove common prefixes: "1. ", "- ", "* ", etc.
                        line.trim()
                            .removePrefix("-")
                            .removePrefix("*")
                            .removePrefix("•")
                            .replace(Regex("^\\d+\\.\\s*"), "") // Remove "1. ", "2. ", etc.
                            .trim()
                    }
            }

            // Strategy 3: Single keyword or space-separated (fallback)
            else -> {
                listOf(resultText.trim())
            }
        }

        // Apply common filtering and deduplication
        return keywords
            .filter { it.isNotBlank() }
            .filter { it.length >= getMinKeywordLength() }
            .distinctBy { it.lowercase() }
            .take(getMaxKeywords())
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
