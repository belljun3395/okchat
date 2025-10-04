package com.okestro.okchat.email.scheduling

import com.okestro.okchat.email.event.EmailEventBus
import com.okestro.okchat.email.event.EmailReceivedEvent
import com.okestro.okchat.email.provider.EmailProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Reactive email polling service
 * Polls email providers on a schedule and publishes events via reactive event bus
 */
@Service
class EmailListenerSchedulingService(
    private val emailProviders: List<EmailProvider>,
    private val emailEventBus: EmailEventBus,
    @Qualifier("emailScheduler") private val emailScheduler: Scheduler
) {
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val isPolling = AtomicBoolean(false)

    @PostConstruct
    fun initialize() {
        if (emailProviders.isEmpty()) {
            logger.warn { "No email providers configured. Email listening is disabled." }
            return
        }

        val providerTypes = emailProviders.joinToString { it.getProviderType().name }
        logger.info { "Email listener service initialized with ${emailProviders.size} provider(s): $providerTypes" }
        logger.info { "Providers will connect when OAuth2 tokens are available" }
        logger.info { "Please authenticate at: /api/email/oauth2/authenticate?username=<your-email>" }
        logger.info { "Event bus initialized with ${emailEventBus.subscriberCount()} subscriber(s)" }
    }

    @PreDestroy
    fun cleanup() {
        logger.info { "Cleaning up email listener service" }

        // Cancel all coroutines
        coroutineScope.cancel()

        // Disconnect all providers
        Flux.fromIterable(emailProviders)
            .flatMap { provider ->
                disconnectProvider(provider)
                    .onErrorResume { e ->
                        logger.error(e) { "Error disconnecting provider ${provider.getProviderType()}" }
                        Mono.empty()
                    }
            }
            .then()
            .block(Duration.ofSeconds(30))
    }

    /**
     * Polls all email providers for new messages
     * Runs on a schedule defined in application.yaml
     * * Note: @Scheduled methods should be blocking or return void.
     * This method blocks until all providers are polled to ensure
     * proper scheduling behavior.
     */
    @Scheduled(
        fixedDelayString = "\${email.polling.interval}",
        initialDelayString = "\${email.polling.initial-delay}"
    )
    fun pollEmails() {
        if (!isPolling.compareAndSet(false, true)) {
            logger.warn { "Previous polling is still running, skipping this cycle" }
            return
        }

        try {
            Flux.fromIterable(emailProviders)
                .flatMap(
                    { provider -> pollProvider(provider) },
                    emailProviders.size // Concurrency = number of providers
                )
                .then()
                .timeout(Duration.ofMinutes(5))
                .doFinally { isPolling.set(false) }
                .block()
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error during email polling" }
            isPolling.set(false)
        }
    }

    /**
     * Polls a single provider for new messages and publishes events
     * Runs on the dedicated email scheduler thread pool
     */
    private fun pollProvider(provider: EmailProvider): Mono<Void> =
        Mono.fromRunnable<Void> {
            try {
                pollProviderBlocking(provider)
            } catch (e: Exception) {
                logger.error(e) { "Error polling emails from ${provider.getProviderType()}" }
            }
        }
            .subscribeOn(emailScheduler)
            .then()

    /**
     * Blocking polling logic - runs on dedicated thread pool
     * Note: This intentionally uses blocking operations since Jakarta Mail is inherently blocking
     */
    private fun pollProviderBlocking(provider: EmailProvider) {
        runBlocking {
            try {
                if (!provider.isConnected()) {
                    logger.warn { "Provider ${provider.getProviderType()} is not connected, attempting to reconnect" }
                    val connected = provider.connect()
                    if (!connected) {
                        logger.error { "Failed to reconnect provider ${provider.getProviderType()}" }
                        return@runBlocking
                    }
                    logger.info { "Successfully reconnected provider ${provider.getProviderType()}" }
                }

                val newMessages = provider.fetchNewMessages()
                logger.info { "Fetched ${newMessages.size} new messages from ${provider.getProviderType()}" }

                if (newMessages.isNotEmpty()) {
                    val events = newMessages.map { message ->
                        EmailReceivedEvent(
                            message = message,
                            providerType = provider.getProviderType()
                        )
                    }

                    // Publish events non-blocking
                    emailEventBus.publishAll(events)
                    logger.info { "Published ${events.size} events for ${provider.getProviderType()}" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error during polling cycle for ${provider.getProviderType()}, will retry on next cycle" }
                // Attempt to disconnect and cleanup on error
                try {
                    provider.disconnect()
                } catch (disconnectError: Exception) {
                    logger.warn(disconnectError) { "Error disconnecting provider ${provider.getProviderType()} after polling failure" }
                }
            }
        }
    }

    /**
     * Disconnects a provider on dedicated thread pool
     */
    private fun disconnectProvider(provider: EmailProvider): Mono<Void> =
        Mono.fromRunnable<Void> {
           runBlocking {
                provider.disconnect()
            }
        }
            .subscribeOn(emailScheduler)
            .then()
}
