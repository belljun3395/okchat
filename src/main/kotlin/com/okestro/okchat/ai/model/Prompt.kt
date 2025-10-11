package com.okestro.okchat.ai.model

/**
 * Base sealed class for all prompts used in extraction services.
 * Provides type safety and extensibility for different prompt types.
 */
sealed class Prompt(
    open val instruction: String,
    open val examples: List<PromptExample>,
    open val message: String,
    open val outputFormat: String = "Keywords:"
) {
    /**
     * Formats the prompt for LLM consumption.
     * Can be overridden by subclasses for custom formatting.
     */
    open fun format(): String {
        return """
            $instruction

            Examples:
            ${examples.joinToString("\n\n") { "Input: \"${it.input}\"\nOutput: \"${it.output}\"" }}

            User query: "$message"

            $outputFormat
        """.trimIndent()
    }

    override fun toString(): String = format()
}

data class PromptExample(
    val input: String,
    val output: String
)

/**
 * Standard format instruction to append to all extraction prompts.
 * Ensures LLM outputs in a parseable format.
 *
 * Note: We instruct the LLM to use comma-separated format for consistency,
 * but the parser includes fallback strategies for other formats in case
 * the LLM doesn't follow instructions perfectly.
 */
const val FORMAT_INSTRUCTION = """

CRITICAL OUTPUT FORMAT:
Return ONLY a comma-separated list of keywords.
DO NOT include explanations, numbering, or bullet points.
DO NOT use newlines between keywords.

Examples of CORRECT format:
- "keyword1, keyword2, keyword3"
- "주간회의록, 회의록, 회의"

Examples of INCORRECT format (DO NOT USE):
- "1. keyword1\n2. keyword2" (numbered)
- "- keyword1\n- keyword2" (bulleted)
- "Here are the keywords: keyword1, keyword2" (explanation)
"""

/**
 * Prompt specifically designed for keyword extraction tasks.
 * Appends FORMAT_INSTRUCTION to ensure consistent output format.
 */
data class KeyWordExtractionPrompt(
    override val instruction: String,
    override val examples: List<PromptExample>,
    override val message: String,
    override val outputFormat: String = "Keywords (comma-separated only):"
) : Prompt("$instruction\n\n$FORMAT_INSTRUCTION", examples, message, outputFormat)
