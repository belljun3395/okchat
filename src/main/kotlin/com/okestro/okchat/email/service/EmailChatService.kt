package com.okestro.okchat.email.service

import com.okestro.okchat.chat.service.DocumentBaseChatService
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
        private const val EMAIL_HEADER = """안녕하세요. 이 메일은 AI 시스템이 자동으로 생성한 답변입니다.

"""

        private const val EMAIL_FOOTER = """

===
위 내용은 Confluence 문서를 기반으로 AI가 자동 생성한 답변입니다.
추가 문의사항이 있으시면 언제든 연락 주시기 바랍니다."""
    }

    /**
     * Process email question and generate email-optimized response
     *
     * @param emailSubject The subject of the email (may be "(제목 없음)" if empty)
     * @param emailContent The content of the email (already cleaned by AbstractEmailProvider)
     * @return Flux of response strings with email header and footer
     */
    fun processEmailQuestion(emailSubject: String, emailContent: String): Flux<String> {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        log.info { "[Email Chat Request] Subject: $emailSubject" }
        log.info { "[Email Content Preview] ${emailContent.take(100)}..." }

        // Additional validation - should not happen due to upstream validation
        if (emailSubject.isBlank() && emailContent.isBlank()) {
            log.error { "Both subject and content are blank - this should not happen" }
            return Flux.just("죄송합니다. 이메일 내용을 읽을 수 없어 답변을 생성할 수 없습니다.")
        }

        // Build question - simple concatenation, let ChatService handle the rest
        val question = buildString {
            if (emailSubject.isNotBlank() && emailSubject != "(제목 없음)") {
                append(emailSubject)
                append(" ")
            }
            append(emailContent)
        }

        log.info { "[Query] Forwarding to ChatService" }
        log.info { "[Query Preview] ${question.take(200)}..." }

        // Get response from ChatService and add email wrapper
        var headerSent = false

        return documentBaseChatService.chat(question, null)
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
