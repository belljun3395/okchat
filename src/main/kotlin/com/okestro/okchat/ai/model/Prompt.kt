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
 * Prompt for structured output tasks where the format is handled by an external parser (e.g., BeanOutputParser).
 * Does NOT append legacy format instructions.
 */
data class StructuredPrompt(
    override val instruction: String,
    override val examples: List<PromptExample>,
    override val message: String,
    // Output format is usually embedded in the instruction by the parser, so we keep this minimal or empty
    override val outputFormat: String = ""
) : Prompt(instruction, examples, message, outputFormat) {
    override fun toString(): String = format()
}
