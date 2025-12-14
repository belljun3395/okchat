package com.okestro.okchat.fixture

/**
 * DSL-style test fixtures for improved readability.
 * Extracted relevant parts for okchat-domain-user.
 */
object TestFixtures {

    // ==================== Email DSL ====================

    @DslMarker
    annotation class EmailDsl

    @EmailDsl
    class EmailBuilder {
        var body: String = "Main content"
        private var signatureName: String? = null
        private var signatureEmail: String? = null
        private var sentFrom: String? = null
        private val quotedLines = mutableListOf<String>()

        fun withSignature(name: String, email: String) {
            this.signatureName = name
            this.signatureEmail = email
        }

        fun withMobileSignature(device: String = "iPhone") {
            this.sentFrom = "Sent from my $device"
        }

        fun addQuotedLine(line: String) {
            quotedLines.add(line)
        }

        fun addQuotedLines(lines: List<String>) {
            quotedLines.addAll(lines)
        }

        fun quotedReply(init: QuotedReplyBuilder.() -> Unit) {
            val reply = QuotedReplyBuilder().apply(init).build()
            quotedLines.addAll(reply)
        }

        fun build(): String {
            val parts = mutableListOf(body)

            if (quotedLines.isNotEmpty()) {
                parts.add("")
                parts.addAll(quotedLines.map { "> $it" })
            }

            if (signatureName != null) {
                parts.add("")
                parts.add("--")
                parts.add(signatureName!!)
                if (signatureEmail != null) {
                    parts.add(signatureEmail!!)
                }
            }

            if (sentFrom != null) {
                parts.add("")
                parts.add(sentFrom!!)
            }

            return parts.joinToString("\n")
        }
    }

    @EmailDsl
    class QuotedReplyBuilder {
        private val lines = mutableListOf<String>()

        fun from(author: String, date: String? = null) {
            val dateStr = date?.let { ", $it" } ?: ""
            lines.add("On$dateStr, $author wrote:")
        }

        fun line(text: String) {
            lines.add(text)
        }

        fun nested(init: QuotedReplyBuilder.() -> Unit) {
            val nestedReply = QuotedReplyBuilder().apply(init).build()
            lines.addAll(nestedReply.map { "> $it" })
        }

        fun build(): List<String> = lines
    }

    fun email(init: EmailBuilder.() -> Unit): String {
        return EmailBuilder().apply(init).build()
    }

    /**
     * Email test samples
     */
    object EmailCleaner {
        val EMAIL_WITH_SIGNATURE = email {
            body = "This is the main content."
            withSignature("John Doe", "john@example.com")
        }

        val EMAIL_WITH_SENT_FROM = email {
            body = "This is the main content."
            withMobileSignature("iPhone")
        }

        val EMAIL_WITH_QUOTED_LINES = email {
            body = "This is my reply."
            addQuotedLine("This is a quoted line")
            addQuotedLine("Another quoted line")
        }

        val COMPLEX_REAL_WORLD_EMAIL = email {
            body = """
                Hi team,
                
                I wanted to follow up on our discussion.
                
                Best regards,
                John
            """.trimIndent()

            quotedReply {
                from("Jane", "2024-01-15")
                line("Thanks for the update.")
                nested {
                    from("Bob", "2024-01-14")
                    line("Here's the original message.")
                }
            }

            withSignature("John Doe", "john@example.com")
            withMobileSignature("iPhone")
        }

        const val SUBJECT_MULTIPLE_RE = "Re: Re: Re: Original Subject"
        const val SUBJECT_MULTIPLE_FWD = "Fwd: Fwd: Original Subject"
        const val SUBJECT_MIXED_PREFIXES = "  Re: Fwd: Original Subject  "
    }
}
