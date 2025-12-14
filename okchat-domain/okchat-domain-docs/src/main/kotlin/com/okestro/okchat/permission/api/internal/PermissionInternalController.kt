package com.okestro.okchat.permission.api.internal

import com.okestro.okchat.permission.api.internal.dto.InternalGetAllowedPathsResponse
import com.okestro.okchat.permission.application.GetAllowedPathsForUserUseCase
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/api/v1/permissions")
class PermissionInternalController(
    private val getAllowedPathsForUserUseCase: GetAllowedPathsForUserUseCase
) {

    @GetMapping("/allowed-paths")
    suspend fun getAllowedPaths(
        @RequestParam email: String,
        @RequestParam(required = false) knowledgeBaseId: Long?
    ): ResponseEntity<InternalGetAllowedPathsResponse> {
        log.debug { "[Internal] Get allowed paths request: email=$email, kbId=$knowledgeBaseId" }

        val input = GetAllowedPathsForUserUseCaseIn(email, knowledgeBaseId)
        val output = getAllowedPathsForUserUseCase.execute(input)

        return ResponseEntity.ok(InternalGetAllowedPathsResponse(output.paths))
    }
}
