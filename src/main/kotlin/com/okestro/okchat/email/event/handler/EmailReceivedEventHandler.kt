package com.okestro.okchat.email.event.handler

import com.okestro.okchat.email.event.EmailEventBus
import com.okestro.okchat.email.event.EmailReceivedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

/**
 * Reactive email event handler
 * Subscribes to the email event bus and processes events in a non-blocking way
 */
@Component
class EmailReceivedEventHandler(
    private val emailEventBus: EmailEventBus
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

        // TODO: Add actual email processing logic here
        // Examples: Save to database, AI analysis, auto-reply, etc.
    }
}
