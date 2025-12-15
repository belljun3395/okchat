package com.okestro.okchat.batch.task

import com.okestro.okchat.confluence.application.ConfluenceSyncUseCase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Confluence sync job.
 *
 * Intended to run as a one-shot batch job (e.g. Kubernetes CronJob) and exit.
 */
@Component
@ConditionalOnProperty(
    name = ["task.confluence-sync.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class ConfluenceSyncTask(
    private val confluenceSyncUseCase: ConfluenceSyncUseCase,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val meterRegistry: MeterRegistry
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) {
        runBlocking(MDCContext()) {
            syncConfluence()
        }
    }

    private suspend fun syncConfluence() {
        log.info { "========== Start Confluence Sync Task ==========" }
        val sample = Timer.start(meterRegistry)
        val tags = Tags.of("task", "confluence-sync")

        try {
            val knowledgeBases = withContext(Dispatchers.IO + MDCContext()) {
                knowledgeBaseRepository.findAllByEnabledTrueAndType(KnowledgeBaseType.CONFLUENCE)
            }

            if (knowledgeBases.isEmpty()) {
                log.warn { "No enabled Confluence Knowledge Bases found. Skipping sync." }
                return
            }

            log.info { "Found ${knowledgeBases.size} Confluence Knowledge Bases to sync..." }

            var successCount = 0
            var failCount = 0

            knowledgeBases.forEach { kb ->
                try {
                    confluenceSyncUseCase.syncKnowledgeBase(kb)
                    successCount++
                    log.info { "Synced KB ${kb.name} successfully" }
                } catch (e: Exception) {
                    failCount++
                    log.error(e) { "Error syncing Knowledge Base ID=${kb.id}, name=${kb.name}" }
                }
            }

            log.info { "========== Confluence Sync Completed ==========" }
            log.info { "Synced KBs: success=$successCount, failed=$failCount" }

            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "success")))
            meterRegistry.counter("task.execution.count", tags.and("status", "success")).increment()
            meterRegistry.counter("task.confluence.sync.kb.success", tags).increment(successCount.toDouble())
            meterRegistry.counter("task.confluence.sync.kb.failed", tags).increment(failCount.toDouble())
        } catch (e: Exception) {
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "failure")))
            meterRegistry.counter("task.execution.count", tags.and("status", "failure")).increment()
            log.error(e) { "Error during Confluence sync task: ${e.message}" }
            throw e // Re-throw to mark task as failed
        }
    }
}
