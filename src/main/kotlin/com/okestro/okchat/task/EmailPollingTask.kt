package com.okestro.okchat.task

import com.okestro.okchat.email.event.EmailEventBus
import com.okestro.okchat.email.event.EmailReceivedEvent
import com.okestro.okchat.email.provider.EmailProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled email polling task with Spring Cloud Task single instance support
 *
 * In multi-server environments, Spring Cloud Task's singleInstanceEnabled prevents
 * multiple instances from executing simultaneously using leader election.
 *
 * Features:
 * - Periodic execution via @Scheduled
 * - Single instance guarantee via Spring Cloud Task (enabled in TaskConfig)
 * - Automatic failover when primary instance fails
 */
@Component
@ConditionalOnProperty(
    name = ["task.email-polling.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class EmailPollingTask(
    private val emailProviders: List<EmailProvider>,
    private val emailEventBus: EmailEventBus,
    private val meterRegistry: MeterRegistry
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    /**
     * CommandLineRunner execution (for one-time task execution)
     */
    override fun run(vararg args: String?) {
        log.info { "EmailPollingTask initialized - scheduled polling enabled" }
    }

    /**
     * Scheduled polling (protected by Spring Cloud Task single instance)
     */
    @Scheduled(
        fixedDelayString = "\${email.polling.interval}",
        initialDelayString = "\${email.polling.initial-delay}"
    )
    fun pollEmails() {
        log.info { "========== Start Email Polling Task ==========" }
        val sample = Timer.start(meterRegistry)
        val tags = Tags.of("task", "email-polling")

        try {
            if (emailProviders.isEmpty()) {
                log.warn { "No email providers configured. Skipping email polling." }
                return
            }

            log.info { "Polling ${emailProviders.size} email provider(s)..." }

            var totalMessages = 0
            var totalEvents = 0

            emailProviders.forEach { provider ->
                val (messages, events) = pollProvider(provider)
                totalMessages += messages
                totalEvents += events
            }

            log.info { "========== Email Polling Completed ==========" }
            log.info { "Total messages fetched: $totalMessages" }
            log.info { "Total events published: $totalEvents" }

            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "success")))
            meterRegistry.counter("task.execution.count", tags.and("status", "success")).increment()
            meterRegistry.counter("task.email.polling.messages.fetched", tags).increment(totalMessages.toDouble())
            meterRegistry.counter("task.email.polling.events.published", tags).increment(totalEvents.toDouble())
        } catch (e: Exception) {
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "failure")))
            meterRegistry.counter("task.execution.count", tags.and("status", "failure")).increment()
            log.error(e) { "Error during email polling: ${e.message}" }
        }
    }

    /**
     * Poll a single email provider and publish events
     * Returns pair of (messages fetched, events published)
     */
    private fun pollProvider(provider: EmailProvider): Pair<Int, Int> = runBlocking {
        val providerType = provider.getProviderType()

        try {
            // Connect if not already connected
            if (!provider.isConnected()) {
                log.info { "Connecting to $providerType..." }
                val connected = provider.connect()
                if (!connected) {
                    log.error { "Failed to connect to $providerType" }
                    return@runBlocking Pair(0, 0)
                }
                log.info { "✓ Connected to $providerType" }
            }

            // Fetch new messages
            val messages = provider.fetchNewMessages()
            log.info { "✓ Fetched ${messages.size} new messages from $providerType" }

            // Publish events if any messages
            if (messages.isNotEmpty()) {
                val events = messages.map { message ->
                    EmailReceivedEvent(
                        message = message,
                        providerType = providerType
                    )
                }

                emailEventBus.publishAll(events)
                log.info { "✓ Published ${events.size} events for $providerType" }

                return@runBlocking Pair(messages.size, events.size)
            }

            Pair(0, 0)
        } catch (e: Exception) {
            log.error(e) { "Error polling $providerType: ${e.message}" }

            // Attempt cleanup on error
            try {
                provider.disconnect()
                log.info { "Disconnected $providerType after error" }
            } catch (disconnectError: Exception) {
                log.warn(disconnectError) { "Error disconnecting $providerType: ${disconnectError.message}" }
            }

            Pair(0, 0)
        }
    }
}
