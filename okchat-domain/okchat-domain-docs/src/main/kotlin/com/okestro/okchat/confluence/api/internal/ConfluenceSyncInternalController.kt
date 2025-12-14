package com.okestro.okchat.confluence.api.internal

import com.okestro.okchat.confluence.api.internal.dto.InternalConfluenceSyncResponse
import com.okestro.okchat.confluence.application.ConfluenceSyncUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val confluenceSyncUseCase: ConfluenceSyncUseCase
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/sync")
    suspend fun sync(): InternalConfluenceSyncResponse {
        return try {
            confluenceSyncUseCase.execute()
            InternalConfluenceSyncResponse(status = "SUCCESS")
        } catch (e: Exception) {
            log.error(e) { "Confluence sync failed: ${e.message}" }
            InternalConfluenceSyncResponse(status = "FAILURE", message = e.message)
        }
    }
}
