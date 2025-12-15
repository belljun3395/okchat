package com.okestro.okchat.batch.task

import com.okestro.okchat.confluence.application.ConfluenceSyncUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["task.confluence-sync.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class ConfluenceSyncTask(
    private val confluenceSyncUseCase: ConfluenceSyncUseCase
) : CommandLineRunner {
    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) {
        runBlocking(MDCContext()) {
            try {
                log.info { "[ConfluenceSync] Starting Confluence sync task" }
                confluenceSyncUseCase.execute()
                log.info { "[ConfluenceSync] Completed successfully" }
            } catch (e: Exception) {
                log.error(e) { "[ConfluenceSync] Failed: ${e.message}" }
                throw e // Re-throw to mark task as failed
            }
        }
    }
}
