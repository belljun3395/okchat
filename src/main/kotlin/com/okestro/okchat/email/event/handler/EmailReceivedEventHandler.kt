package com.okestro.okchat.email.event.handler

import com.okestro.okchat.email.application.SavePendingReplyUseCase
import com.okestro.okchat.email.application.dto.SavePendingReplyUseCaseIn
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
 * Automatically analyzes email content and creates pending replies for review before sending
 */
@Component
class EmailReceivedEventHandler(
    private val emailEventBus: EmailEventBus,
    private val emailChatService: EmailChatService,
    private val emailReplyService: EmailReplyService,
    private val savePendingReplyUseCase: SavePendingReplyUseCase
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
     * Process the email question and save pending reply for review instead of sending immediately
     */
    private suspend fun processAndReply(event: EmailReceivedEvent) {
        val message = event.message
        logger.info { "Processing email question: ${message.subject}" }

        // Validate email has meaningful content
        val hasSubject = message.subject.isNotBlank()
        val hasContent = message.content.isNotBlank()

        if (!hasSubject && !hasContent) {
            logger.warn { "[EmailHandler] Email has no content: from=${message.from}, saving error response for review" }
            // Create error response for review (instead of sending immediately)
            val errorResponse = buildString {
                appendLine("Hello.")
                appendLine()
                appendLine("We received your email, but both the subject and content are empty, so we cannot generate a response.")
                appendLine("Please resend with your question content, and we'll be happy to help.")
                appendLine()
                appendLine("Thank you.")
            }

            // Save for review instead of sending
            savePendingReplyUseCase.execute(
                SavePendingReplyUseCaseIn(
                    originalMessage = message,
                    replyContent = errorResponse,
                    providerType = event.providerType,
                    toEmail = message.to.firstOrNull() ?: "unknown",
                    knowledgeBaseId = event.knowledgeBaseId
                )
            )
            logger.info { "[EmailHandler] Empty content error response saved for review: from=${message.from}" }
            return
        }

        // Use subject as content if content is empty
        val effectiveSubject = if (hasSubject) message.subject else "(No subject)"
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
                originalContent = message.content.ifBlank { "(No content)" }
            )

            // Save reply for review instead of sending immediately
            savePendingReplyUseCase.execute(
                SavePendingReplyUseCaseIn(
                    originalMessage = message,
                    replyContent = replyContent,
                    providerType = event.providerType,
                    toEmail = message.to.firstOrNull() ?: "unknown",
                    knowledgeBaseId = event.knowledgeBaseId
                )
            )
            logger.info { "AI-generated reply saved for review: from=${message.from}, subject=${message.subject}" }
        } else {
            logger.warn { "No AI response generated for email: ${message.subject}" }
        }
    }
}
