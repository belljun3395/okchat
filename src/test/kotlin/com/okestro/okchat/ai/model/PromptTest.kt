package com.okestro.okchat.ai.model

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertTrue

@DisplayName("Prompt Tests")
class PromptTest {

    companion object {
        @JvmStatic
        fun promptContentTestCases() = listOf(
            Arguments.of("User query:", "user query section"),
            Arguments.of("Examples:", "examples section")
        )
    }

    @ParameterizedTest(name = "should include {1}")
    @MethodSource("promptContentTestCases")
    @DisplayName("should include required sections in formatted prompt")
    fun `should include required sections`(expectedContent: String, description: String) {
        // given
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = emptyList(),
            message = "Test message"
        )

        // when
        val formatted = prompt.format()

        // then
        formatted shouldContain expectedContent
    }

    @Test
    @DisplayName("should include user instruction")
    fun `should include user instruction`() {
        // given
        val instruction = "Extract technical keywords from the message"
        val prompt = StructuredPrompt(
            instruction = instruction,
            examples = emptyList(),
            message = "Test message"
        )

        // when
        val formatted = prompt.format()

        // then
        formatted shouldContain instruction
    }

    @Test
    @DisplayName("should include examples")
    fun `should include examples`() {
        // given
        val examples = listOf(
            PromptExample("input1", "output1"),
            PromptExample("input2", "output2")
        )
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // when
        val formatted = prompt.format()

        // then
        formatted shouldContain "input1"
        formatted shouldContain "output1"
        formatted shouldContain "input2"
        formatted shouldContain "output2"
    }

    @Test
    @DisplayName("should include message")
    fun `should include message`() {
        // given
        val message = "Find documents about Spring Boot"
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = emptyList(),
            message = message
        )

        // when
        val formatted = prompt.format()

        // then
        formatted shouldContain message
        formatted shouldContain "User query:"
    }

    @Test
    @DisplayName("should format examples correctly")
    fun `should format examples correctly`() {
        // given
        val examples = listOf(
            PromptExample("Spring Boot tutorials", "spring, boot, tutorial")
        )
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // when
        val formatted = prompt.format()

        // then
        formatted shouldContain "Input: \"Spring Boot tutorials\""
        formatted shouldContain "Output: \"spring, boot, tutorial\""
    }

    @Test
    @DisplayName("should return formatted prompt from toString")
    fun `should return formatted prompt from toString`() {
        // given
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = emptyList(),
            message = "Test message"
        )

        // when
        val toString = prompt.toString()
        val formatted = prompt.format()

        // then
        assertTrue(toString == formatted)
    }

    @Test
    @DisplayName("should handle Korean examples")
    fun `should handle Korean examples`() {
        // given
        val examples = listOf(
            PromptExample("백엔드 개발 레포 정보", "백엔드, backend, 개발, development")
        )
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // when
        val formatted = prompt.format()

        // then
        formatted shouldContain "백엔드 개발 레포 정보"
        formatted shouldContain "백엔드, backend, 개발, development"
    }

    @Test
    @DisplayName("should handle multiple examples")
    fun `should handle multiple examples`() {
        // given
        val examples = listOf(
            PromptExample("input1", "output1"),
            PromptExample("input2", "output2"),
            PromptExample("input3", "output3")
        )
        val prompt = StructuredPrompt(
            instruction = "Extract keywords",
            examples = examples,
            message = "Test message"
        )

        // when
        val formatted = prompt.format()

        // then
        examples.forEach { example ->
            formatted shouldContain example.input
            formatted shouldContain example.output
        }
    }

    @Test
    @DisplayName("should create well-structured prompt")
    fun `should create well-structured prompt`() {
        // given
        val instruction = "Extract technical keywords"
        val examples = listOf(
            PromptExample("Spring Boot REST API", "spring, boot, rest, api")
        )
        val message = "How to build microservices?"

        // when
        val prompt = StructuredPrompt(instruction, examples, message)
        val formatted = prompt.format()

        // then
        val lines = formatted.lines()
        assertTrue(lines.isNotEmpty())
        // Should have all major sections
        formatted shouldContain instruction
        formatted shouldContain "Examples:"
        formatted shouldContain "User query:"
    }
}
