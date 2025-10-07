package com.okestro.okchat.ai.support

/**
 * Standard prompt templates for extraction services.
 * Ensures consistent output format across all LLM extraction operations.
 */
object PromptTemplate {

    /**
     * Standard format instruction to append to all extraction prompts.
     * Ensures LLM outputs in a parseable format.
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
     * Build a standard extraction prompt with consistent formatting.
     * * @param instruction The main extraction instruction
     * @param examples Example inputs and outputs
     * @param userMessage The user's message to extract from
     */
    fun buildExtractionPrompt(
        instruction: String,
        examples: String,
        userMessage: String
    ): String {
        return """
$instruction

$examples

$FORMAT_INSTRUCTION

User query: "$userMessage"

Keywords (comma-separated only):
        """.trimIndent()
    }

    /**
     * Standard examples section format
     */
    fun formatExamples(vararg examples: Pair<String, String>): String {
        return buildString {
            appendLine("EXAMPLES:")
            examples.forEach { (input, output) ->
                appendLine("- Input: \"$input\"")
                appendLine("  Output: \"$output\"")
            }
        }
    }
}
