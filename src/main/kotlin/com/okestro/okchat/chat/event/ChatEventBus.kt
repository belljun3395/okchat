package com.okestro.okchat.chat.event

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger {}

/**
 * Reactive event bus for chat-related events using Reactor Sinks
 * * Provides non-blocking, backpressure-aware event publishing for:
 * - Chat interaction analytics
 * - Conversation history persistence
 * - User feedback processing
 * * Uses multicast sink with backpressure buffer to handle high-throughput scenarios
 */
@Component
class ChatEventBus {
    private val sink: Sinks.Many<ChatEvent> = Sinks.many()
        .multicast()
        .onBackpressureBuffer()

    /**
     * Publish a chat event
     * * Non-blocking operation that emits the event to all subscribers.
     * Events are processed asynchronously by ChatEventHandler.
     * * @param event to publish
     */
    fun publish(event: ChatEvent) {
        val result = sink.tryEmitNext(event)
        if (result.isFailure) {
            logger.error { "Failed to publish chat event: $result, event: ${event::class.simpleName}" }
        } else {
            logger.debug { "Published chat event: ${event::class.simpleName}" }
        }
    }

    /**
     * Subscribe to chat events
     * * @return Flux stream of chat events
     */
    fun subscribe(): Flux<ChatEvent> = sink.asFlux()
}
