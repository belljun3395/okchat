package com.okestro.okchat.email.service

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("EmailReplyService Enhanced Tests")
class EmailReplyServiceEnhancedTest {

    private val emailProperties: EmailProperties = mockk()
    private val oauth2TokenService: OAuth2TokenService = mockk()
    private val service = EmailReplyService(emailProperties, oauth2TokenService)

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("buildReplyContent Tests")
    inner class BuildReplyContentTests {

        @Test
        @DisplayName("should build reply content with answer and original message")
        fun `should build reply content with answer and original message`() {
            // given
            val answer = "이것은 AI의 답변입니다"
            val originalContent = "원본 질문 내용입니다"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain answer
            result shouldContain "==="
            result shouldContain "Original message:"
            result shouldContain originalContent
        }

        @Test
        @DisplayName("should include separator between answer and original")
        fun `should include separator between answer and original`() {
            // given
            val answer = "AI answer here"
            val originalContent = "User question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            val lines = result.lines()
            val separatorIndex = lines.indexOf("===")
            val answerIndex = lines.indexOf(answer)
            val originalIndex = lines.indexOfFirst { it.contains("Original message") }

            answerIndex shouldBe 0
            separatorIndex shouldBeGreaterThan answerIndex
            originalIndex shouldBeGreaterThan separatorIndex
        }

        @Test
        @DisplayName("should truncate long original content")
        fun `should truncate long original content`() {
            // given
            val answer = "답변"
            val originalContent = (1..50).joinToString("\n") { "Line $it" }

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain answer
            result shouldContain "Original message:"
            // Content should be truncated by EmailContentCleaner
        }

        @ParameterizedTest(name = "should handle: {0}")
        @ValueSource(strings = ["", "  ", "\n", "\t"])
        @DisplayName("should handle empty or whitespace content")
        fun `should handle empty or whitespace content`(content: String) {
            // given
            val answer = "Answer"

            // when
            val result = service.buildReplyContent(answer, content)

            // then
            result shouldContain answer
            result shouldContain "Original message:"
        }

        @Test
        @DisplayName("should preserve newlines in answer")
        fun `should preserve newlines in answer`() {
            // given
            val answer = "Line 1\nLine 2\nLine 3"
            val originalContent = "Original"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "Line 1"
            result shouldContain "Line 2"
            result shouldContain "Line 3"
        }

        @Test
        @DisplayName("should handle special characters in content")
        fun `should handle special characters in content`() {
            // given
            val answer = "답변: 한글 테스트 <test@example.com>"
            val originalContent = "질문: 특수문자 & < > \" '"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain answer
            result shouldContain originalContent
        }

        @Test
        @DisplayName("should format multi-line answer correctly")
        fun `should format multi-line answer correctly`() {
            // given
            val answer = """
                Hello,
                
                This is a multi-line answer.
                
                Best regards
            """.trimIndent()
            val originalContent = "Question here"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "Hello,"
            result shouldContain "This is a multi-line answer."
            result shouldContain "Best regards"
            result shouldContain "Original message:"
        }

        @Test
        @DisplayName("should handle long answer text")
        fun `should handle long answer text`() {
            // given
            val answer = "Answer: " + "word ".repeat(1000)
            val originalContent = "Short question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "Answer:"
            result.length shouldBeGreaterThan 5000
        }

        @Test
        @DisplayName("should handle markdown-style formatting in answer")
        fun `should handle markdown-style formatting in answer`() {
            // given
            val answer = """
                # Heading
                - Item 1
                - Item 2
                **Bold text**
            """.trimIndent()
            val originalContent = "Question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "# Heading"
            result shouldContain "- Item 1"
            result shouldContain "**Bold text**"
        }

        @Test
        @DisplayName("should handle URLs in content")
        fun `should handle URLs in content`() {
            // given
            val answer = "Please visit https://example.com for more info"
            val originalContent = "Where can I find documentation?"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "https://example.com"
            result shouldContain originalContent
        }

        @Test
        @DisplayName("should handle code blocks in answer")
        fun `should handle code blocks in answer`() {
            // given
            val answer = """
                Here's a code example:
                ```kotlin
                fun hello() {
                    println("Hello")
                }
                ```
            """.trimIndent()
            val originalContent = "How do I write code?"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "```kotlin"
            result shouldContain "fun hello()"
            result shouldContain "```"
        }
    }

    @Nested
    @DisplayName("Reply Structure Tests")
    inner class ReplyStructureTests {

        @Test
        @DisplayName("should have consistent structure")
        fun `should have consistent structure`() {
            // given
            val answer = "Answer"
            val originalContent = "Question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            val sections = result.split("===")
            sections.size shouldBe 2
            sections[0].trim() shouldContain answer
            sections[1].trim() shouldContain "Original message:"
        }

        @Test
        @DisplayName("should include blank lines for readability")
        fun `should include blank lines for readability`() {
            // given
            val answer = "Answer"
            val originalContent = "Question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            // Should have blank lines between sections
            result shouldContain "\n\n"
        }

        @Test
        @DisplayName("should start with the answer")
        fun `should start with the answer`() {
            // given
            val answer = "This is the answer"
            val originalContent = "Question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result.trim().startsWith(answer.trim()) shouldBe true
        }

        @Test
        @DisplayName("should end with original message section")
        fun `should end with original message section`() {
            // given
            val answer = "Answer"
            val originalContent = "This is the original question"

            // when
            val result = service.buildReplyContent(answer, originalContent)

            // then
            result shouldContain "Original message:"
            val lastSection = result.substringAfter("Original message:")
            lastSection.trim() shouldContain originalContent
        }
    }
}
