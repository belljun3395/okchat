package com.okestro.okchat.email.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailContentCleanerTest {

    @Test
    fun `cleanEmailContent should remove standard signature separator`() {
        // Given
        val content = """
            This is the main content.
            
            --
            John Doe
            john@example.com
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertTrue(cleaned.contains("This is the main content."))
        assertFalse(cleaned.contains("John Doe"))
        assertFalse(cleaned.contains("john@example.com"))
    }

    @Test
    fun `cleanEmailContent should remove 'Sent from' signatures`() {
        // Given
        val content = """
            This is the main content.
            
            Sent from my iPhone
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertTrue(cleaned.contains("This is the main content."))
        assertFalse(cleaned.contains("Sent from my iPhone"))
    }

    @Test
    fun `cleanEmailContent should remove 'Get Outlook' signatures`() {
        // Given
        val content = """
            This is the main content.
            
            Get Outlook for iOS
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertTrue(cleaned.contains("This is the main content."))
        assertFalse(cleaned.contains("Get Outlook"))
    }

    @Test
    fun `cleanEmailContent should remove quoted lines starting with greater than`() {
        // Given
        val content = """
            This is my reply.
            
            > This is a quoted line
            > Another quoted line
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertTrue(cleaned.contains("This is my reply."))
        assertFalse(cleaned.contains("This is a quoted line"))
        assertFalse(cleaned.contains("Another quoted line"))
    }

    @Test
    fun `cleanEmailContent should remove forwarded message separators`() {
        // Given
        val content = """
            This is my message.
            
            ---------- Forwarded message ---------
            From: Someone
            Original content
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertTrue(cleaned.contains("This is my message."))
        // The separator line itself should be removed
        assertFalse(cleaned.contains("---------- Forwarded message ---------"))
    }

    @Test
    fun `cleanEmailContent should remove original message separators`() {
        // Given
        val content = """
            This is my reply.
            
            -----Original Message-----
            From: Someone
            Sent: Monday
            Original content
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertTrue(cleaned.contains("This is my reply."))
        // The separator line itself should be removed
        assertFalse(cleaned.contains("-----Original Message-----"))
    }

    @Test
    fun `cleanEmailContent should normalize excessive whitespace`() {
        // Given
        val content = """
            This is    content with     extra spaces.
            
            
            
            And too many newlines.
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(content)

        // Then
        assertFalse(cleaned.contains("    ")) // no multiple spaces
        assertFalse(cleaned.contains("\n\n\n")) // no triple newlines
    }

    @Test
    fun `convertHtmlToText should parse HTML content`() {
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
        assertTrue(text.contains("This is bold text."))
        assertTrue(text.contains("Link"))
        assertFalse(text.contains("<html>"))
        assertFalse(text.contains("<p>"))
    }

    @Test
    fun `convertHtmlToText should handle invalid HTML gracefully`() {
        // Given
        val invalidHtml = "Not really HTML <unclosed tag"

        // When
        val text = EmailContentCleaner.convertHtmlToText(invalidHtml)

        // Then - should return something, even if parsing fails
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun `cleanSubject should normalize Re prefix`() {
        // Given
        val subject1 = "Re: Re: Re: Original Subject"
        val subject2 = "RE: re: Original Subject"

        // When
        val cleaned1 = EmailContentCleaner.cleanSubject(subject1)
        val cleaned2 = EmailContentCleaner.cleanSubject(subject2)

        // Then
        assertEquals("Re: Original Subject", cleaned1)
        assertEquals("Re: Original Subject", cleaned2)
    }

    @Test
    fun `cleanSubject should normalize Fwd prefix`() {
        // Given
        val subject1 = "Fwd: Fwd: Original Subject"
        val subject2 = "Fw: Original Subject"

        // When
        val cleaned1 = EmailContentCleaner.cleanSubject(subject1)
        val cleaned2 = EmailContentCleaner.cleanSubject(subject2)

        // Then
        assertEquals("Fwd: Original Subject", cleaned1)
        assertEquals("Fwd: Original Subject", cleaned2)
    }

    @Test
    fun `cleanSubject should handle mixed prefixes`() {
        // Given
        val subject = "  Re: Fwd: Original Subject  "

        // When
        val cleaned = EmailContentCleaner.cleanSubject(subject)

        // Then
        assertEquals("Re: Fwd: Original Subject", cleaned)
    }

    @Test
    fun `isContentMeaningful should validate content length`() {
        // Given
        val meaningfulContent = "This is a meaningful email with enough content."
        val shortContent = "Hi"
        val emptyContent = ""

        // Then
        assertTrue(EmailContentCleaner.isContentMeaningful(meaningfulContent))
        assertFalse(EmailContentCleaner.isContentMeaningful(shortContent))
        assertFalse(EmailContentCleaner.isContentMeaningful(emptyContent))
    }

    @Test
    fun `isContentMeaningful should use custom minimum length`() {
        // Given
        val content = "Short"

        // Then
        assertTrue(EmailContentCleaner.isContentMeaningful(content, minLength = 3))
        assertFalse(EmailContentCleaner.isContentMeaningful(content, minLength = 10))
    }

    @Test
    fun `truncateForReply should limit lines`() {
        // Given
        val lines = (1..20).map { "Line $it" }.joinToString("\n")

        // When
        val truncated = EmailContentCleaner.truncateForReply(lines, maxLines = 5)

        // Then
        assertTrue(truncated.contains("Line 1"))
        assertTrue(truncated.contains("Line 5"))
        assertFalse(truncated.contains("Line 6"))
        assertTrue(truncated.contains("Original message partially omitted"))
    }

    @Test
    fun `truncateForReply should not truncate short content`() {
        // Given
        val content = "Line 1\nLine 2\nLine 3"

        // When
        val truncated = EmailContentCleaner.truncateForReply(content, maxLines = 10)

        // Then
        assertEquals(content, truncated)
        assertFalse(truncated.contains("omitted"))
    }

    @Test
    fun `cleanEmailContent should handle complex real-world email`() {
        // Given
        val realWorldEmail = """
            Hi team,
            
            I wanted to follow up on our discussion.
            
            Best regards,
            John
            
            > On 2024-01-15, Jane wrote:
            > Thanks for the update.
            > > On 2024-01-14, Bob wrote:
            > > Here's the original message.
            
            --
            John Doe
            Software Engineer
            john@example.com
            
            Sent from my iPhone
        """.trimIndent()

        // When
        val cleaned = EmailContentCleaner.cleanEmailContent(realWorldEmail)

        // Then
        assertTrue(cleaned.contains("I wanted to follow up"))
        // Quoted lines starting with > should be removed
        assertFalse(cleaned.contains("> On 2024-01-15"))
        assertFalse(cleaned.contains("> Thanks for the update"))
        // Signature and device info should be removed
        assertFalse(cleaned.contains("Sent from my iPhone"))
        // Main content should remain
        assertTrue(cleaned.contains("Hi team"))
    }
}
