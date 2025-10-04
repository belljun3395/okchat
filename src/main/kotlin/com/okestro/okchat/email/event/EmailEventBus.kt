package com.okestro.okchat.email.event

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger {}

/**
 * Reactive event bus for email events using Reactor Sinks
 * This provides a non-blocking, backpressure-aware event publishing mechanism
 */
@Component
class EmailEventBus {
    private val sink: Sinks.Many<EmailReceivedEvent> = Sinks.many()
        .multicast()
        .onBackpressureBuffer()

    /**
     * Publish an email received event
     * Non-blocking operation that emits the event to all subscribers
     */
    fun publish(event: EmailReceivedEvent) {
        val result = sink.tryEmitNext(event)
        if (result.isFailure) {
            logger.error { "Failed to publish email event: $result, event: ${event.message.subject}" }
        }
    }

    /**
     * Publish multiple events
     * Simplified to avoid unnecessary reactive overhead since tryEmitNext is already non-blocking
     */
    fun publishAll(events: List<EmailReceivedEvent>) {
        events.forEach { event ->
            val result = sink.tryEmitNext(event)
            if (result.isFailure) {
                logger.error { "Failed to publish email event: $result, event: ${event.message.subject}" }
            }
        }
    }

    /**
     * Subscribe to email received events
     * Returns a Flux that emits events as they arrive
     */
    fun subscribe(): Flux<EmailReceivedEvent> = sink.asFlux()

    /**
     * Get the current number of subscribers
     */
    fun subscriberCount(): Int = sink.currentSubscriberCount()
}
