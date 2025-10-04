package com.okestro.okchat.email.event.handler

import com.okestro.okchat.email.event.EmailEventBus
import com.okestro.okchat.email.event.EmailReceivedEvent
import com.okestro.okchat.email.service.EmailChatService
import com.okestro.okchat.email.service.EmailReplyService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

/**
 * Reactive email event handler
 * Subscribes to the email event bus and processes events in a non-blocking way
 * Automatically analyzes email content and sends AI-powered replies based on Confluence documentation
 */
@Component
class EmailReceivedEventHandler(
    private val emailEventBus: EmailEventBus,
    private val emailChatService: EmailChatService,
    private val emailReplyService: EmailReplyService
) {
    @PostConstruct
    fun subscribeToEvents() {
        emailEventBus.subscribe()
            .publishOn(Schedulers.boundedElastic())
            .doOnNext { event -> handleEmailReceived(event) }
            .doOnError { e -> logger.error(e) { "Error processing email event" } }
            .retry(Long.MAX_VALUE) // Continue retrying indefinitely
            .doOnError { e -> logger.warn { "Retrying email event processing after error: ${e.message}" } }
            .subscribe()

        logger.info { "Email event handler subscribed to event bus" }
    }

    private fun handleEmailReceived(event: EmailReceivedEvent) {
        logger.info {
            """
            ===== New Email Received =====
            Provider: ${event.providerType}
            From: ${event.message.from}
            To: ${event.message.to.joinToString(", ")}
            Subject: ${event.message.subject}
            Received: ${event.message.receivedDate}
            Content Preview: ${event.message.content.take(100)}${if (event.message.content.length > 100) "..." else ""}
            Timestamp: ${event.timestamp}
            ==============================
            """.trimIndent()
        }

        // Process email asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processAndReply(event)
            } catch (e: Exception) {
                logger.error(e) { "Failed to process and reply to email: ${event.message.subject}" }
            }
        }
    }

    /**
     * Process the email question and send an AI-powered reply based on Confluence documentation
     */
    private suspend fun processAndReply(event: EmailReceivedEvent) {
        val message = event.message
        logger.info { "Processing email question: ${message.subject}" }

        // Validate email has meaningful content
        val hasSubject = message.subject.isNotBlank()
        val hasContent = message.content.isNotBlank()

        if (!hasSubject && !hasContent) {
            logger.warn { "Email has no subject and no content, skipping auto-reply: from ${message.from}" }
            // Send a polite error response
            val errorResponse = buildString {
                appendLine("안녕하세요.")
                appendLine()
                appendLine("귀하의 이메일을 받았으나, 제목과 내용이 비어있어 답변을 생성할 수 없습니다.")
                appendLine("질문 내용을 포함하여 다시 보내주시면 감사하겠습니다.")
                appendLine()
                appendLine("감사합니다.")
            }
            emailReplyService.sendReply(message, errorResponse, event.providerType)
            logger.info { "Empty content error response sent to: ${message.from}" }
            return
        }

        // Use subject as content if content is empty
        val effectiveSubject = if (hasSubject) message.subject else "(제목 없음)"
        val effectiveContent = if (hasContent) message.content else message.subject

        logger.info { "Effective subject: $effectiveSubject, has content: $hasContent" }

        // Use EmailChatService to process email question
        val aiResponse = StringBuilder()
        emailChatService.processEmailQuestion(effectiveSubject, effectiveContent)
            .doOnNext { chunk ->
                aiResponse.append(chunk)
            }
            .doOnComplete {
                logger.info { "Email-optimized AI response generated successfully for: ${message.subject}" }
            }
            .doOnError { error ->
                logger.error(error) { "Error generating AI response for email: ${message.subject}" }
            }
            .blockLast() // Wait for completion

        if (aiResponse.isNotEmpty()) {
            // Build reply content
            val replyContent = emailReplyService.buildReplyContent(
                answer = aiResponse.toString(),
                originalContent = message.content.ifBlank { "(내용 없음)" }
            )

            // Send reply email with provider type
            emailReplyService.sendReply(message, replyContent, event.providerType)
            logger.info { "Auto-reply sent successfully to: ${message.from}" }
        } else {
            logger.warn { "No AI response generated for email: ${message.subject}" }
        }
    }
}
