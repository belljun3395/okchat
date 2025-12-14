package com.okestro.okchat.email.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup

private val logger = KotlinLogging.logger {}

/**
 * Utility object for cleaning and preprocessing email content
 */
object EmailContentCleaner {

    /**
     * Clean email content by removing signatures, quoted text, and normalizing whitespace
     */
    fun cleanEmailContent(content: String): String {
        var cleaned = content

        // Remove email signatures
        cleaned = removeSignatures(cleaned)

        // Remove quoted text and reply chains
        cleaned = removeQuotedText(cleaned)

        // Normalize whitespace
        cleaned = normalizeWhitespace(cleaned)

        return cleaned.trim()
    }

    /**
     * Convert HTML content to plain text
     */
    fun convertHtmlToText(html: String): String {
        return try {
            Jsoup.parse(html).text()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse HTML content" }
            html // Return raw HTML as fallback
        }
    }

    /**
     * Remove common email signatures
     */
    private fun removeSignatures(content: String): String {
        var result = content

        // Pattern 1: Standard signature separator "--"
        result = result.replace(Regex("(?m)^--\\s*$.*", RegexOption.DOT_MATCHES_ALL), "")

        // Pattern 2: "Sent from my..." patterns
        result = result.replace(
            Regex("(?i)Sent from my (iPhone|iPad|Android|Mobile Device|Samsung).*", RegexOption.DOT_MATCHES_ALL),
            ""
        )

        // Pattern 3: "Get Outlook for..." patterns
        result = result.replace(
            Regex("(?i)Get Outlook for (iOS|Android|Mobile).*", RegexOption.DOT_MATCHES_ALL),
            ""
        )

        // Pattern 4: Common signature starters
        val signatureStarters = listOf(
            "Best regards",
            "Kind regards",
            "Sincerely",
            "Thanks",
            "Thank you",
            "Regards",
            "Cheers"
        )

        for (starter in signatureStarters) {
            // Remove everything after common signature starters if followed by name/email
            result = result.replace(
                Regex("(?i)$starter,?\\s*\n.*", RegexOption.DOT_MATCHES_ALL),
                ""
            )
        }

        return result
    }

    /**
     * Remove quoted text from email replies
     */
    private fun removeQuotedText(content: String): String {
        return content.lines()
            .filterNot { line ->
                val trimmed = line.trimStart()

                // Remove lines starting with ">"
                trimmed.startsWith(">") ||

                    // Remove "On [date], [person] wrote:" patterns
                    trimmed.matches(Regex("^On .+wrote:.*")) ||

                    // Remove forwarded message separators
                    trimmed.contains("---------- Forwarded message ---------") ||
                    trimmed.contains("-----Original Message-----") ||

                    // Remove Gmail quote indicators
                    trimmed.contains("On \\w{3}, \\w{3} \\d{1,2}, \\d{4} at \\d{1,2}:\\d{2}".toRegex()) ||

                    // Remove Outlook quote indicators
                    trimmed.matches(Regex("^From:.*")) && content.contains("Sent:") ||

                    // Remove thread separators
                    trimmed.startsWith("________________________________") ||
                    trimmed.startsWith("=====")
            }
            .joinToString("\n")
    }

    /**
     * Normalize whitespace in email content
     */
    private fun normalizeWhitespace(content: String): String {
        return content
            // Replace 3+ consecutive newlines with 2 newlines
            .replace(Regex("\\n{3,}"), "\n\n")
            // Replace 2+ consecutive spaces with 1 space
            .replace(Regex(" {2,}"), " ")
            // Remove spaces at the end of lines
            .replace(Regex(" +\n"), "\n")
            // Trim
            .trim()
    }

    /**
     * Clean email subject line
     */
    fun cleanSubject(subject: String): String {
        return subject
            // Normalize "Re:" prefix (remove duplicates)
            .replace(Regex("^(Re:\\s*)+", RegexOption.IGNORE_CASE), "Re: ")
            // Normalize "Fwd:" prefix (remove duplicates)
            .replace(Regex("^(Fwd:\\s*)+", RegexOption.IGNORE_CASE), "Fwd: ")
            // Normalize "Fw:" to "Fwd:"
            .replace(Regex("^Fw:", RegexOption.IGNORE_CASE), "Fwd:")
            .trim()
    }

    /**
     * Validate email content - check if meaningful content exists
     */
    fun isContentMeaningful(content: String, minLength: Int = 10): Boolean {
        val cleaned = cleanEmailContent(content)
        return cleaned.length >= minLength
    }

    /**
     * Truncate long content for inclusion in replies
     */
    fun truncateForReply(content: String, maxLines: Int = 10): String {
        val lines = content.lines()
        return if (lines.size > maxLines) {
            lines.take(maxLines).joinToString("\n") + "\n\n[... Original message partially omitted ...]"
        } else {
            content
        }
    }
}
