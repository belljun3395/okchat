package com.okestro.okchat.confluence.api.internal

import com.okestro.okchat.confluence.api.internal.dto.InternalConfluenceSyncResponse
import com.okestro.okchat.confluence.application.ConfluenceSyncUseCase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/api/v1/confluence")
@ConditionalOnProperty(
    name = ["task.confluence-sync.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class ConfluenceSyncInternalController(
    private val confluenceSyncUseCase: ConfluenceSyncUseCase,
    private val knowledgeBaseRepository: KnowledgeBaseRepository
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/sync")
    suspend fun sync(): InternalConfluenceSyncResponse {
        return try {
            val knowledgeBases = withContext(Dispatchers.IO + MDCContext()) {
                knowledgeBaseRepository.findAllByEnabledTrueAndType(KnowledgeBaseType.CONFLUENCE)
            }

            if (knowledgeBases.isEmpty()) {
                log.warn { "No enabled Confluence Knowledge Bases found." }
                return InternalConfluenceSyncResponse(status = "SUCCESS", message = "No enabled Confluence KBs found")
            }

            var successCount = 0
            var failCount = 0

            knowledgeBases.forEach { kb ->
                try {
                    confluenceSyncUseCase.syncKnowledgeBase(kb)
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    log.error(e) { "Error syncing KB ${kb.name}: ${e.message}" }
                }
            }

            InternalConfluenceSyncResponse(
                status = "SUCCESS",
                message = "Synced $successCount KBs, failed $failCount"
            )
        } catch (e: Exception) {
            log.error(e) { "Confluence sync failed: ${e.message}" }
            InternalConfluenceSyncResponse(status = "FAILURE", message = e.message)
        }
    }
}
