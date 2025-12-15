package com.okestro.okchat.email.service

import com.okestro.okchat.chat.service.DocumentBaseChatService
import com.okestro.okchat.chat.service.dto.ChatServiceRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

private val log = KotlinLogging.logger {}

/**
 * Email-specific chat service (Facade for ChatService)
 * Adds email-specific headers and footers to ChatService responses
 */
@Service
class EmailChatService(
    private val documentBaseChatService: DocumentBaseChatService
) {

    companion object {
        private const val EMAIL_HEADER = """Hello. This email is an automatically generated response from the AI system.

"""

        private const val EMAIL_FOOTER = """

===
The above content is an automatically generated response by AI based on Confluence documents.
If you have any additional questions, please feel free to contact us anytime."""
    }

    /**
     * Process email question and generate email-optimized response
     *
     * @param emailSubject The subject of the email (may be "(No subject)" if empty)
     * @param emailContent The content of the email (already cleaned by AbstractEmailProvider)
     * @return Flux of response strings with email header and footer
     */
    suspend fun processEmailQuestion(emailSubject: String, emailContent: String): Flux<String> {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        log.info { "[Email Chat Request] Subject: $emailSubject" }
        log.info { "[Email Content Preview] ${emailContent.take(100)}..." }

        // Additional validation - should not happen due to upstream validation
        if (emailSubject.isBlank() && emailContent.isBlank()) {
            log.error { "Both subject and content are blank - this should not happen" }
            return Flux.just("Sorry. Cannot read email content and therefore cannot generate a response.")
        }

        // Build question - simple concatenation, let ChatService handle the rest
        val question = buildString {
            if (emailSubject.isNotBlank() && emailSubject != "(No subject)") {
                append(emailSubject)
                append(" ")
            }
            append(emailContent)
        }

        log.info { "[Query] Forwarding to ChatService" }
        log.info { "[Query Preview] ${question.take(200)}..." }

        // Get response from ChatService and add email wrapper
        // Note: Email-based chats don't maintain session history
        var headerSent = false

        return documentBaseChatService.chat(
            ChatServiceRequest(
                message = question,
                isDeepThink = true
            )
        )
            .map { chunk ->
                if (!headerSent) {
                    headerSent = true
                    EMAIL_HEADER + chunk
                } else {
                    chunk
                }
            }
            .concatWith(Flux.just(EMAIL_FOOTER))
            .doOnComplete {
                log.info { "[Email Chat Completed] Response with email wrapper sent" }
                log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
            }
            .doOnError { error ->
                log.error(error) { "[Email Chat Error] ${error.message}" }
                log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
            }
    }
}
