package com.okestro.okchat.permission.application

import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseIn
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseOut
import com.okestro.okchat.search.application.SearchAllPathsUseCase
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseIn
import com.okestro.okchat.search.model.AllowedKnowledgeBases
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class GetAllowedPathsForUserUseCase(
    private val userClient: UserClient,
    private val knowledgeMemberClient: KnowledgeMemberClient,
    private val searchAllPathsUseCase: SearchAllPathsUseCase
) {
    suspend fun execute(useCaseIn: GetAllowedPathsForUserUseCaseIn): GetAllowedPathsForUserUseCaseOut {
        val email = useCaseIn.email
        val caller = userClient.getByEmail(email)
            ?: throw IllegalArgumentException("Caller not found: $email")

        val scope: AllowedKnowledgeBases = if (caller.role == "SYSTEM_ADMIN") {
            if (useCaseIn.knowledgeBaseId != null) {
                AllowedKnowledgeBases.Subset(listOf(useCaseIn.knowledgeBaseId))
            } else {
                AllowedKnowledgeBases.All
            }
        } else {
            val memberships = knowledgeMemberClient.getMembershipsByUserId(caller.id)
            val spaceAdminKbIds =
                memberships.filter { it.role == "ADMIN" }.map { it.knowledgeBaseId }.toSet()

            if (spaceAdminKbIds.isEmpty()) {
                throw IllegalAccessException("Insufficient permissions for user: $email")
            }

            if (useCaseIn.knowledgeBaseId != null) {
                if (!spaceAdminKbIds.contains(useCaseIn.knowledgeBaseId)) {
                    throw IllegalAccessException("User $email does not have permission for Knowledge Base ${useCaseIn.knowledgeBaseId}")
                }
                AllowedKnowledgeBases.Subset(listOf(useCaseIn.knowledgeBaseId))
            } else {
                AllowedKnowledgeBases.Subset(spaceAdminKbIds.toList())
            }
        }

        log.info { "Get allowed paths for $email (Allowed KBs: $scope)" }
        val result = searchAllPathsUseCase.execute(SearchAllPathsUseCaseIn(scope))
        return GetAllowedPathsForUserUseCaseOut(result.paths)
    }
}
