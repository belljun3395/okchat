package com.okestro.okchat.email.service

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.provider.EmailMessage
import com.okestro.okchat.email.util.EmailContentCleaner
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Service
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Service for sending email replies using SMTP with OAuth2
 */
@Service
class EmailReplyService(
    private val emailProperties: EmailProperties,
    private val oauth2TokenService: OAuth2TokenService
) {

    private val parser = Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()

    private val renderer = HtmlRenderer.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()

    /**
     * Send a reply email
     *
     * @param originalMessage The original email message to reply to
     * @param replyContent The content of the reply
     * @param providerType The email provider type (GMAIL or OUTLOOK)
     */
    suspend fun sendReply(
        originalMessage: EmailMessage,
        replyContent: String,
        providerType: EmailProperties.EmailProviderType
    ) = withContext(Dispatchers.IO + MDCContext()) {
        try {
            logger.info { "Sending reply via $providerType to email: ${originalMessage.subject}" }

            // Find provider configuration
            val providerConfig = emailProperties.providers.values.find { it.type == providerType }
                ?: throw IllegalStateException("Provider configuration not found for $providerType")

            // Get OAuth2 access token
            val accessToken = oauth2TokenService.getAccessToken(providerConfig.username).awaitSingle()
                ?: throw IllegalStateException("OAuth2 token not available for ${providerConfig.username}")

            // Create SMTP session
            val session = createSmtpSession(providerConfig)

            // Create reply message
            val replyMessage = MimeMessage(session)
            replyMessage.setFrom(InternetAddress(providerConfig.username))
            replyMessage.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(originalMessage.from)
            )

            // Set subject with "Re:" prefix if not already present
            val subject = if (originalMessage.subject.startsWith("Re:", ignoreCase = true)) {
                originalMessage.subject
            } else {
                "Re: ${originalMessage.subject}"
            }
            replyMessage.subject = subject

            // Set In-Reply-To and References headers for threading
            val messageId = originalMessage.rawMessage.getHeader("Message-ID")?.firstOrNull()
            if (messageId != null) {
                replyMessage.setHeader("In-Reply-To", messageId)
                replyMessage.setHeader("References", messageId)
            }

            // Convert markdown to HTML
            val htmlContent = renderer.render(parser.parse(replyContent))

            // Set content
            replyMessage.setContent(htmlContent, "text/html; charset=utf-8")
            replyMessage.saveChanges()

            // Send the message
            session.transport.use { transport ->
                transport.connect(
                    getSmtpHost(providerType),
                    providerConfig.username,
                    accessToken
                )
                transport.sendMessage(replyMessage, replyMessage.allRecipients)
                logger.info { "Reply sent successfully to ${originalMessage.from}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send reply email to ${originalMessage.from}" }
            throw e
        }
    }

    /**
     * Create SMTP session with OAuth2 authentication
     */
    private fun createSmtpSession(
        config: EmailProperties.EmailProviderConfig
    ): Session {
        val props = Properties().apply {
            // SMTP configuration
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2")

            // Provider-specific SMTP host and port
            when (config.type) {
                EmailProperties.EmailProviderType.GMAIL -> {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }
                EmailProperties.EmailProviderType.OUTLOOK -> {
                    put("mail.smtp.host", "smtp.office365.com")
                    put("mail.smtp.port", "587")
                }
            }

            // OAuth2 authentication
            put("mail.smtp.auth.mechanisms", "XOAUTH2")
            put("mail.smtp.auth.login.disable", "true")
            put("mail.smtp.auth.plain.disable", "true")

            // Timeouts
            put("mail.smtp.timeout", config.mail.timeout.toString())
            put("mail.smtp.connectiontimeout", config.mail.connectionTimeout.toString())

            // Debug (optional)
            if (config.mail.debug) {
                put("mail.debug", "true")
                put("mail.debug.auth", "true")
            }
        }

        return Session.getInstance(props)
    }

    /**
     * Get SMTP host for the provider
     */
    private fun getSmtpHost(providerType: EmailProperties.EmailProviderType): String {
        return when (providerType) {
            EmailProperties.EmailProviderType.GMAIL -> "smtp.gmail.com"
            EmailProperties.EmailProviderType.OUTLOOK -> "smtp.office365.com"
        }
    }

    /**
     * Build a reply content with the AI-generated answer and original message context
     */
    fun buildReplyContent(answer: String, originalContent: String): String {
        // Truncate original content to avoid very long emails
        val truncatedOriginal = EmailContentCleaner.truncateForReply(originalContent, maxLines = 10)

        return buildString {
            appendLine(answer)
            appendLine()
            appendLine("===")
            appendLine("Original message:")
            appendLine(truncatedOriginal)
        }
    }
}
