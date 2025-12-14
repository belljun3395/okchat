package com.okestro.okchat.batch.task

import com.okestro.okchat.batch.client.docs.DocsClient
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
    private val docsClient: DocsClient
) : CommandLineRunner {
    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) {
        runBlocking(MDCContext()) {
            try {
                log.info { "[ConfluenceSync] Triggering sync via docs internal API" }
                val response = docsClient.syncConfluence()
                log.info { "[ConfluenceSync] Completed: status=${response.status} message=${response.message}" }
            } catch (e: Exception) {
                log.error(e) { "[ConfluenceSync] Failed: ${e.message}" }
            }
        }
    }
}
