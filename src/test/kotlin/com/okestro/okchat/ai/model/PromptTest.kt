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
            Arguments.of("CRITICAL OUTPUT FORMAT", "format instruction"),
            Arguments.of("comma-separated", "output format"),
            Arguments.of("User query:", "user query section"),
            Arguments.of("Keywords (comma-separated only):", "output section")
        )
    }

    @ParameterizedTest(name = "should include {1}")
    @MethodSource("promptContentTestCases")
    @DisplayName("should include required sections in formatted prompt")
    fun `should include required sections`(expectedContent: String, description: String) {
        // given
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = instruction,
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
        val prompt = KeyWordExtractionPrompt(
            userInstruction = "Extract keywords",
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
    @DisplayName("should contain critical instructions in FORMAT_INSTRUCTION")
    fun `should contain critical instructions in FORMAT_INSTRUCTION`() {
        // then
        FORMAT_INSTRUCTION shouldContain "CRITICAL OUTPUT FORMAT"
        FORMAT_INSTRUCTION shouldContain "comma-separated"
        FORMAT_INSTRUCTION shouldContain "DO NOT"
    }

    @Test
    @DisplayName("should provide correct format examples in FORMAT_INSTRUCTION")
    fun `should provide correct format examples in FORMAT_INSTRUCTION`() {
        // then
        FORMAT_INSTRUCTION shouldContain "Examples of CORRECT format"
        FORMAT_INSTRUCTION shouldContain "keyword1, keyword2, keyword3"
    }

    @Test
    @DisplayName("should provide incorrect format examples in FORMAT_INSTRUCTION")
    fun `should provide incorrect format examples in FORMAT_INSTRUCTION`() {
        // then
        FORMAT_INSTRUCTION shouldContain "Examples of INCORRECT format"
        FORMAT_INSTRUCTION shouldContain "DO NOT USE"
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
        val prompt = KeyWordExtractionPrompt(instruction, examples, message)
        val formatted = prompt.format()

        // then
        val lines = formatted.lines()
        assertTrue(lines.isNotEmpty())
        // Should have all major sections
        formatted shouldContain instruction
        formatted shouldContain "Examples:"
        formatted shouldContain "CRITICAL OUTPUT FORMAT:"
        formatted shouldContain "User query:"
        formatted shouldContain "Keywords (comma-separated only):"
    }
}
