package com.okestro.okchat.batch.task

import com.okestro.okchat.batch.client.docs.DocsClient
import com.okestro.okchat.batch.client.user.UserEmailClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Email polling job.
 *
 * Intended to run as a one-shot batch job (e.g. Kubernetes CronJob) and exit.
 */
@Component
@ConditionalOnProperty(
    name = ["task.email-polling.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class EmailPollingTask(
    private val docsClient: DocsClient,
    private val userEmailClient: UserEmailClient,
    private val meterRegistry: MeterRegistry
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) {
        runBlocking(MDCContext()) {
            pollEmails()
        }
    }

    private suspend fun pollEmails() {
        log.info { "========== Start Email Polling Task ==========" }
        val sample = Timer.start(meterRegistry)
        val tags = Tags.of("task", "email-polling")

        try {
            val knowledgeBases = docsClient.getEnabledKnowledgeBases()

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
                    val result = userEmailClient.pollEmails(kb.id)
                    if (result.messagesCount > 0 || result.eventsCount > 0) {
                        processedKbs++
                        totalMessages += result.messagesCount
                        totalEvents += result.eventsCount
                        log.info { "Polled KB ${kb.name}: ${result.messagesCount} messages, ${result.eventsCount} events" }
                    }
                } catch (e: Exception) {
                    log.error(e) { "Error processing Knowledge Base ID=${kb.id}" }
                }
            }

            log.info { "========== Email Polling Completed ==========" }
            log.info { "Processed KBs with activity: $processedKbs" }
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
}
