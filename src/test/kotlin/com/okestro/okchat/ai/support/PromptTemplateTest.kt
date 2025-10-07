package com.okestro.okchat.ai.support

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PromptTemplateTest {

    @Test
    fun `FORMAT_INSTRUCTION should contain critical instructions`() {
        // Then
        assertTrue(PromptTemplate.FORMAT_INSTRUCTION.contains("CRITICAL OUTPUT FORMAT"))
        assertTrue(PromptTemplate.FORMAT_INSTRUCTION.contains("comma-separated"))
        assertTrue(PromptTemplate.FORMAT_INSTRUCTION.contains("DO NOT"))
    }

    @Test
    fun `FORMAT_INSTRUCTION should provide examples`() {
        // Then
        assertTrue(PromptTemplate.FORMAT_INSTRUCTION.contains("Examples of CORRECT format"))
        assertTrue(PromptTemplate.FORMAT_INSTRUCTION.contains("Examples of INCORRECT format"))
    }

    @Test
    fun `buildExtractionPrompt should combine all parts`() {
        // Given
        val instruction = "Extract keywords from the following text."
        val examples = "Example: Input -> Output"
        val userMessage = "Find documents about Spring Boot"

        // When
        val prompt = PromptTemplate.buildExtractionPrompt(instruction, examples, userMessage)

        // Then
        assertTrue(prompt.contains(instruction))
        assertTrue(prompt.contains(examples))
        assertTrue(prompt.contains(PromptTemplate.FORMAT_INSTRUCTION))
        assertTrue(prompt.contains(userMessage))
        assertTrue(prompt.contains("User query:"))
    }

    @Test
    fun `buildExtractionPrompt should format user message in quotes`() {
        // Given
        val userMessage = "Find documents about Spring Boot"

        // When
        val prompt = PromptTemplate.buildExtractionPrompt(
            instruction = "Extract keywords",
            examples = "Example",
            userMessage = userMessage
        )

        // Then
        assertTrue(prompt.contains("\"$userMessage\""))
    }

    @Test
    fun `formatExamples should create examples section`() {
        // Given
        val examples = arrayOf(
            "Spring Boot tutorials" to "spring, boot, tutorial",
            "Java programming guide" to "java, programming, guide"
        )

        // When
        val formatted = PromptTemplate.formatExamples(*examples)

        // Then
        assertTrue(formatted.contains("EXAMPLES:"))
        assertTrue(formatted.contains("Input: \"Spring Boot tutorials\""))
        assertTrue(formatted.contains("Output: \"spring, boot, tutorial\""))
        assertTrue(formatted.contains("Input: \"Java programming guide\""))
        assertTrue(formatted.contains("Output: \"java, programming, guide\""))
    }

    @Test
    fun `formatExamples should handle single example`() {
        // Given
        val example = arrayOf("test input" to "test output")

        // When
        val formatted = PromptTemplate.formatExamples(*example)

        // Then
        assertTrue(formatted.contains("EXAMPLES:"))
        assertTrue(formatted.contains("test input"))
        assertTrue(formatted.contains("test output"))
    }

    @Test
    fun `formatExamples should handle empty examples`() {
        // When
        val formatted = PromptTemplate.formatExamples()

        // Then
        assertTrue(formatted.contains("EXAMPLES:"))
    }

    @Test
    fun `buildExtractionPrompt should produce well-structured output`() {
        // Given
        val instruction = "Extract technical keywords"
        val examples = PromptTemplate.formatExamples(
            "Spring Boot REST API" to "spring, boot, rest, api"
        )
        val userMessage = "How to build microservices?"

        // When
        val prompt = PromptTemplate.buildExtractionPrompt(instruction, examples, userMessage)

        // Then - verify structure
        val lines = prompt.lines()
        assertTrue(lines.isNotEmpty())
        // Should have all major sections
        assertTrue(prompt.contains("Extract technical keywords"))
        assertTrue(prompt.contains("EXAMPLES:"))
        assertTrue(prompt.contains("CRITICAL OUTPUT FORMAT:"))
        assertTrue(prompt.contains("User query:"))
        assertTrue(prompt.contains("Keywords (comma-separated only):"))
    }
}
