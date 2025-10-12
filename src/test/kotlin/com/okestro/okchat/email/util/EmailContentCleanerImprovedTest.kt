package com.okestro.okchat.email.util

import com.okestro.okchat.fixture.TestFixtures
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * Improved test for EmailContentCleaner using Kotest.
 * Demonstrates comprehensive parameterized testing for email cleaning.
 */
class EmailContentCleanerImprovedTest : DescribeSpec({

    describe("EmailContentCleaner - cleanEmailContent") {

        context("signature removal") {

            data class SignatureTestCase(
                val description: String,
                val content: String,
                val shouldContain: String,
                val shouldNotContain: List<String>
            )

            withData(
                nameFn = { it.description },
                SignatureTestCase(
                    description = "Standard signature separator",
                    content = TestFixtures.EmailCleaner.EMAIL_WITH_SIGNATURE,
                    shouldContain = "This is the main content.",
                    shouldNotContain = listOf("John Doe", "john@example.com")
                ),
                SignatureTestCase(
                    description = "'Sent from' signatures",
                    content = TestFixtures.EmailCleaner.EMAIL_WITH_SENT_FROM,
                    shouldContain = "This is the main content.",
                    shouldNotContain = listOf("Sent from my iPhone")
                ),
                SignatureTestCase(
                    description = "'Get Outlook' signatures",
                    content = """
                        This is the main content.
                        
                        Get Outlook for iOS
                    """.trimIndent(),
                    shouldContain = "This is the main content.",
                    shouldNotContain = listOf("Get Outlook")
                )
            ) { (_, content, shouldContain, shouldNotContain) ->
                // When
                val cleaned = EmailContentCleaner.cleanEmailContent(content)

                // Then
                cleaned shouldContain shouldContain
                shouldNotContain.forEach { text ->
                    cleaned shouldNotContain text
                }
            }
        }

        context("quoted lines and forwarded messages") {

            it("should remove quoted lines starting with greater than") {
                // Given
                val content = TestFixtures.EmailCleaner.EMAIL_WITH_QUOTED_LINES

                // When
                val cleaned = EmailContentCleaner.cleanEmailContent(content)

                // Then
                cleaned shouldContain "This is my reply."
                cleaned shouldNotContain "This is a quoted line"
                cleaned shouldNotContain "Another quoted line"
            }

            data class ForwardedMessageTestCase(
                val description: String,
                val content: String,
                val separatorToRemove: String
            )

            withData(
                nameFn = { it.description },
                ForwardedMessageTestCase(
                    description = "Forwarded message separator",
                    content = """
                        This is my message.
                        
                        ---------- Forwarded message ---------
                        From: Someone
                        Original content
                    """.trimIndent(),
                    separatorToRemove = "---------- Forwarded message ---------"
                ),
                ForwardedMessageTestCase(
                    description = "Original message separator",
                    content = """
                        This is my reply.
                        
                        -----Original Message-----
                        From: Someone
                        Sent: Monday
                        Original content
                    """.trimIndent(),
                    separatorToRemove = "-----Original Message-----"
                )
            ) { (_, content, separatorToRemove) ->
                // When
                val cleaned = EmailContentCleaner.cleanEmailContent(content)

                // Then
                cleaned shouldContain "This is my"
                cleaned shouldNotContain separatorToRemove
            }
        }

        context("whitespace normalization") {

            it("should normalize excessive whitespace") {
                // Given
                val content = """
                    This is    content with     extra spaces.
                    
                    
                    
                    And too many newlines.
                """.trimIndent()

                // When
                val cleaned = EmailContentCleaner.cleanEmailContent(content)

                // Then
                cleaned shouldNotContain "    "
                cleaned shouldNotContain "\n\n\n"
            }
        }

        context("complex real-world scenarios") {

            it("should handle complex real-world email") {
                // Given
                val content = TestFixtures.EmailCleaner.COMPLEX_REAL_WORLD_EMAIL

                // When
                val cleaned = EmailContentCleaner.cleanEmailContent(content)

                // Then
                cleaned shouldContain "I wanted to follow up"
                cleaned shouldNotContain "> On 2024-01-15"
                cleaned shouldNotContain "> Thanks for the update"
                cleaned shouldNotContain "Sent from my iPhone"
                cleaned shouldContain "Hi team"
            }
        }
    }

    describe("EmailContentCleaner - convertHtmlToText") {

        it("should parse HTML content") {
            // Given
            val html = """
                <html>
                    <body>
                        <p>This is <strong>bold</strong> text.</p>
                        <a href="http://example.com">Link</a>
                    </body>
                </html>
            """.trimIndent()

            // When
            val text = EmailContentCleaner.convertHtmlToText(html)

            // Then
            text shouldContain "This is bold text."
            text shouldContain "Link"
            text shouldNotContain "<html>"
            text shouldNotContain "<p>"
        }

        it("should handle invalid HTML gracefully") {
            // Given
            val invalidHtml = "Not really HTML <unclosed tag"

            // When
            val text = EmailContentCleaner.convertHtmlToText(invalidHtml)

            // Then
            text shouldNotBe ""
        }
    }

    describe("EmailContentCleaner - cleanSubject") {

        data class SubjectTestCase(
            val description: String,
            val input: String,
            val expected: String
        )

        withData(
            nameFn = { it.description },
            SubjectTestCase(
                description = "Multiple Re prefixes",
                input = TestFixtures.EmailCleaner.SUBJECT_MULTIPLE_RE,
                expected = "Re: Original Subject"
            ),
            SubjectTestCase(
                description = "Case insensitive Re",
                input = "RE: re: Original Subject",
                expected = "Re: Original Subject"
            ),
            SubjectTestCase(
                description = "Multiple Fwd prefixes",
                input = TestFixtures.EmailCleaner.SUBJECT_MULTIPLE_FWD,
                expected = "Fwd: Original Subject"
            ),
            SubjectTestCase(
                description = "Fw to Fwd normalization",
                input = "Fw: Original Subject",
                expected = "Fwd: Original Subject"
            ),
            SubjectTestCase(
                description = "Mixed prefixes",
                input = TestFixtures.EmailCleaner.SUBJECT_MIXED_PREFIXES,
                expected = "Re: Fwd: Original Subject"
            )
        ) { (_, input, expected) ->
            // When
            val cleaned = EmailContentCleaner.cleanSubject(input)

            // Then
            cleaned shouldBe expected
        }
    }

    describe("EmailContentCleaner - isContentMeaningful") {

        data class MeaningfulContentTestCase(
            val description: String,
            val content: String,
            val minLength: Int,
            val expected: Boolean
        )

        withData(
            nameFn = { it.description },
            MeaningfulContentTestCase(
                description = "Meaningful content",
                content = "This is a meaningful email with enough content.",
                minLength = 20,
                expected = true
            ),
            MeaningfulContentTestCase(
                description = "Short content with default min",
                content = "Hi",
                minLength = 20,
                expected = false
            ),
            MeaningfulContentTestCase(
                description = "Empty content",
                content = "",
                minLength = 20,
                expected = false
            ),
            MeaningfulContentTestCase(
                description = "Short content with custom min",
                content = "Short",
                minLength = 3,
                expected = true
            ),
            MeaningfulContentTestCase(
                description = "Short content exceeds custom min",
                content = "Short",
                minLength = 10,
                expected = false
            )
        ) { (_, content, minLength, expected) ->
            // When
            val result = EmailContentCleaner.isContentMeaningful(content, minLength)

            // Then
            result shouldBe expected
        }
    }

    describe("EmailContentCleaner - truncateForReply") {

        it("should limit lines to maxLines") {
            // Given
            val lines = (1..20).joinToString("\n") { "Line $it" }

            // When
            val truncated = EmailContentCleaner.truncateForReply(lines, maxLines = 5)

            // Then
            truncated shouldContain "Line 1"
            truncated shouldContain "Line 5"
            truncated shouldNotContain "Line 6"
            truncated shouldContain "Original message partially omitted"
        }

        it("should not truncate short content") {
            // Given
            val content = "Line 1\nLine 2\nLine 3"

            // When
            val truncated = EmailContentCleaner.truncateForReply(content, maxLines = 10)

            // Then
            truncated shouldBe content
            truncated shouldNotContain "omitted"
        }
    }
})
