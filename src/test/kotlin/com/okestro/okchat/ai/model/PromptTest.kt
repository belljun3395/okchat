package com.okestro.okchat.ai.model

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PromptTest {

    @Test
    fun `KeyWordExtractionPrompt should include FORMAT_INSTRUCTION`() {
        // Given
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = emptyList(),
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains("CRITICAL OUTPUT FORMAT"))
        assertTrue(formatted.contains("comma-separated"))
    }

    @Test
    fun `KeyWordExtractionPrompt should include instruction`() {
        // Given
        val instruction = "Extract technical keywords from the message"
        val prompt = KeyWordExtractionPrompt(
            userInstruction = instruction,
            examples = emptyList(),
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains(instruction))
    }

    @Test
    fun `KeyWordExtractionPrompt should include examples`() {
        // Given
        val examples = listOf(
            PromptExample("input1", "output1"),
            PromptExample("input2", "output2")
        )
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains("input1"))
        assertTrue(formatted.contains("output1"))
        assertTrue(formatted.contains("input2"))
        assertTrue(formatted.contains("output2"))
    }

    @Test
    fun `KeyWordExtractionPrompt should include message`() {
        // Given
        val message = "Find documents about Spring Boot"
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = emptyList(),
            message = message
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains(message))
        assertTrue(formatted.contains("User query:"))
    }

    @Test
    fun `KeyWordExtractionPrompt should include output format`() {
        // Given
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = emptyList(),
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains("Keywords (comma-separated only):"))
    }

    @Test
    fun `KeyWordExtractionPrompt should format examples correctly`() {
        // Given
        val examples = listOf(
            PromptExample("Spring Boot tutorials", "spring, boot, tutorial")
        )
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains("Input: \"Spring Boot tutorials\""))
        assertTrue(formatted.contains("Output: \"spring, boot, tutorial\""))
    }

    @Test
    fun `KeyWordExtractionPrompt toString should return formatted prompt`() {
        // Given
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = emptyList(),
            message = "Test message"
        )

        // When
        val toString = prompt.toString()
        val formatted = prompt.format()

        // Then
        assertTrue(toString == formatted)
    }

    @Test
    fun `KeyWordExtractionPrompt should handle Korean examples`() {
        // Given
        val examples = listOf(
            PromptExample("백엔드 개발 레포 정보", "백엔드, backend, 개발, development")
        )
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        assertTrue(formatted.contains("백엔드 개발 레포 정보"))
        assertTrue(formatted.contains("백엔드, backend, 개발, development"))
    }

    @Test
    fun `KeyWordExtractionPrompt should handle multiple examples`() {
        // Given
        val examples = listOf(
            PromptExample("input1", "output1"),
            PromptExample("input2", "output2"),
            PromptExample("input3", "output3")
        )
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // When
        val formatted = prompt.format()

        // Then
        examples.forEach { example ->
            assertTrue(formatted.contains(example.input))
            assertTrue(formatted.contains(example.output))
        }
    }

    @Test
    fun `FORMAT_INSTRUCTION should contain critical instructions`() {
        // Then
        assertTrue(FORMAT_INSTRUCTION.contains("CRITICAL OUTPUT FORMAT"))
        assertTrue(FORMAT_INSTRUCTION.contains("comma-separated"))
        assertTrue(FORMAT_INSTRUCTION.contains("DO NOT"))
    }

    @Test
    fun `FORMAT_INSTRUCTION should provide correct format examples`() {
        // Then
        assertTrue(FORMAT_INSTRUCTION.contains("Examples of CORRECT format"))
        assertTrue(FORMAT_INSTRUCTION.contains("keyword1, keyword2, keyword3"))
    }

    @Test
    fun `FORMAT_INSTRUCTION should provide incorrect format examples`() {
        // Then
        assertTrue(FORMAT_INSTRUCTION.contains("Examples of INCORRECT format"))
        assertTrue(FORMAT_INSTRUCTION.contains("DO NOT USE"))
    }

    @Test
    fun `KeyWordExtractionPrompt should create well-structured prompt`() {
        // Given
        val instruction = "Extract technical keywords"
        val examples = listOf(
            PromptExample("Spring Boot REST API", "spring, boot, rest, api")
        )
        val message = "How to build microservices?"

        // When
        val prompt = KeyWordExtractionPrompt(instruction, examples, message)
        val formatted = prompt.format()

        // Then
        val lines = formatted.lines()
        assertTrue(lines.isNotEmpty())
        // Should have all major sections
        assertTrue(formatted.contains(instruction))
        assertTrue(formatted.contains("Examples:"))
        assertTrue(formatted.contains("CRITICAL OUTPUT FORMAT:"))
        assertTrue(formatted.contains("User query:"))
        assertTrue(formatted.contains("Keywords (comma-separated only):"))
    }
}
