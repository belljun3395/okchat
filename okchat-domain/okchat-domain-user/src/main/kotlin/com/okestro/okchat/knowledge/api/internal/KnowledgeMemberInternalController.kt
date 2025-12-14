package com.okestro.okchat.knowledge.api.internal

import com.okestro.okchat.knowledge.api.internal.dto.InternalKnowledgeBaseMembershipResponse
import com.okestro.okchat.knowledge.application.GetKnowledgeBaseMembershipUseCase
import com.okestro.okchat.knowledge.application.GetKnowledgeBaseMembershipsByUserIdUseCase
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembershipUseCaseIn
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembershipsByUserIdUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Internal API for knowledge-member access from other domains.
 *
 * NOTE: This API is intended for server-to-server calls only.
 */
@RestController
@RequestMapping("/internal/api/v1")
class KnowledgeMemberInternalController(
    private val getKnowledgeBaseMembershipUseCase: GetKnowledgeBaseMembershipUseCase,
    private val getKnowledgeBaseMembershipsByUserIdUseCase: GetKnowledgeBaseMembershipsByUserIdUseCase
) {

    @GetMapping("/knowledge-bases/{kbId}/members/{userId}")
    suspend fun getMembership(
        @PathVariable kbId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<InternalKnowledgeBaseMembershipResponse> {
        log.debug { "[Internal] Get KB membership: kbId=$kbId, userId=$userId" }
        val output = getKnowledgeBaseMembershipUseCase.execute(
            GetKnowledgeBaseMembershipUseCaseIn(kbId = kbId, userId = userId)
        )
        val membership = output.membership
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.ok(InternalKnowledgeBaseMembershipResponse.from(membership))
    }

    @GetMapping("/users/{userId}/knowledge-bases")
    suspend fun getMembershipsByUserId(
        @PathVariable userId: Long
    ): List<InternalKnowledgeBaseMembershipResponse> {
        log.debug { "[Internal] Get KB memberships by user: userId=$userId" }
        val output = getKnowledgeBaseMembershipsByUserIdUseCase.execute(
            GetKnowledgeBaseMembershipsByUserIdUseCaseIn(userId = userId)
        )
        return output.memberships.map(InternalKnowledgeBaseMembershipResponse::from)
    }
}
