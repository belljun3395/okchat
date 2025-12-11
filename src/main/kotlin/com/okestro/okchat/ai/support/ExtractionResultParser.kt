package com.okestro.okchat.ai.support

/**
 * Interface for parsing LLM extraction results.
 * Allows for dependency injection and easier testing.
 */
interface ExtractionResultParser {
    fun parse(
        resultText: String?,
        minLength: Int,
        maxKeywords: Int,
        emptyResult: List<String>
    ): List<String>
}

/**
 * Default implementation of ResultParser.
 * Supports multiple format strategies for robustness.
 */
class DefaultExtractionResultParser : ExtractionResultParser {
    /**
     * Parse the LLM result text into a list of keywords.
     *
     * Primary strategy: Comma-separated (as instructed by FORMAT_INSTRUCTION)
     * Fallback strategies: Handle cases where LLM doesn't follow instructions
     *
     * Supports multiple formats for robustness:
     * 1. Comma-separated: "keyword1, keyword2, keyword3" (preferred)
     * 2. Newline-separated: "keyword1\nkeyword2\nkeyword3"
     * 3. Numbered list: "1. keyword1\n2. keyword2"
     * 4. Bulleted list: "- keyword1\n- keyword2"
     */
    override fun parse(
        resultText: String?,
        minLength: Int,
        maxKeywords: Int,
        emptyResult: List<String>
    ): List<String> {
        if (resultText.isNullOrBlank()) {
            return emptyResult
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
        val filtered = keywords
            .filter { it.isNotBlank() }
            .filter { it.length >= minLength }
            .distinctBy { it.lowercase() }
            .take(maxKeywords)

        // Return emptyResult if no valid keywords found
        return filtered.ifEmpty { emptyResult }
    }
}
