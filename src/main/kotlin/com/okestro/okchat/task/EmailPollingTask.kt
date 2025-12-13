package com.okestro.okchat.task

import com.okestro.okchat.email.event.EmailEventBus
import com.okestro.okchat.email.event.EmailReceivedEvent
import com.okestro.okchat.email.provider.EmailProvider
import com.okestro.okchat.email.provider.EmailProviderFactory
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
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
 * - Per-KnowledgeBase polling
 */
@Component
@ConditionalOnProperty(
    name = ["task.email-polling.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class EmailPollingTask(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val knowledgeBaseEmailRepository: com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository,
    private val emailProviderFactory: EmailProviderFactory,
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
        fixedDelayString = "\${email.polling.interval:60000}",
        initialDelayString = "\${email.polling.initial-delay:10000}"
    )
    fun pollEmails() {
        log.info { "========== Start Email Polling Task ==========" }
        val sample = Timer.start(meterRegistry)
        val tags = Tags.of("task", "email-polling")

        try {
            val knowledgeBases = knowledgeBaseRepository.findAllByEnabledTrue()

            if (knowledgeBases.isEmpty()) {
                log.info { "No enabled Knowledge Bases found. Skipping email polling." }
                return
            }

            log.info { "Checking ${knowledgeBases.size} enabled Knowledge Bases..." }

            var totalMessages = 0
            var totalEvents = 0
            var processedKbs = 0

            knowledgeBases.forEach { kb ->
                try {
                    val emailConfigs = knowledgeBaseEmailRepository.findAllByKnowledgeBaseId(kb.id!!)

                    if (emailConfigs.isEmpty()) {
                        return@forEach
                    }

                    processedKbs++
                    log.info { "Polling emails for Knowledge Base: ${kb.name} (ID: ${kb.id})" }

                    emailConfigs.forEach { emailConfig ->
                        // Create provider instance dynamically
                        val pollingConfig = emailConfig.toEmailProviderConfig()
                        val provider = emailProviderFactory.createProvider(pollingConfig)

                        if (provider != null) {
                            val (messages, events) = pollProvider(provider, kb.id!!)
                            totalMessages += messages
                            totalEvents += events
                        }
                    }
                } catch (e: Exception) {
                    log.error(e) { "Error processing Knowledge Base ID=${kb.id}" }
                }
            }

            log.info { "========== Email Polling Completed ==========" }
            log.info { "Processed KBs: $processedKbs" }
            log.info { "Total messages fetched: $totalMessages" }
            log.info { "Total events published: $totalEvents" }

            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "success")))
            meterRegistry.counter("task.execution.count", tags.and("status", "success")).increment()
            meterRegistry.counter("task.email.polling.messages.fetched", tags).increment(totalMessages.toDouble())
            meterRegistry.counter("task.email.polling.events.published", tags).increment(totalEvents.toDouble())
        } catch (e: Exception) {
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "failure")))
            meterRegistry.counter("task.execution.count", tags.and("status", "failure")).increment()
            log.error(e) { "Error during email polling task: ${e.message}" }
        }
    }

    /**
     * Poll a single email provider and publish events
     * Returns pair of (messages fetched, events published)
     */
    private fun pollProvider(provider: EmailProvider, knowledgeBaseId: Long): Pair<Int, Int> = runBlocking {
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
                        providerType = providerType,
                        knowledgeBaseId = knowledgeBaseId
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
