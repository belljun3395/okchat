package com.okestro.okchat.permission.application

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseIn
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseOut
import com.okestro.okchat.search.application.SearchAllPathsUseCase
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseIn
import com.okestro.okchat.search.model.AllowedKnowledgeBases
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import com.okestro.okchat.user.model.entity.UserRole
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class GetAllowedPathsForUserUseCase(
    private val findUserByEmailUseCase: FindUserByEmailUseCase,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository,
    private val searchAllPathsUseCase: SearchAllPathsUseCase
) {
    suspend fun execute(useCaseIn: GetAllowedPathsForUserUseCaseIn): GetAllowedPathsForUserUseCaseOut {
        val email = useCaseIn.email
        val caller = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email)).user
            ?: throw IllegalArgumentException("Caller not found: $email")

        val scope: AllowedKnowledgeBases = if (caller.role == UserRole.SYSTEM_ADMIN) {
            if (useCaseIn.knowledgeBaseId != null) {
                AllowedKnowledgeBases.Subset(listOf(useCaseIn.knowledgeBaseId))
            } else {
                AllowedKnowledgeBases.All
            }
        } else {
            val memberships = withContext(Dispatchers.IO + MDCContext()) {
                knowledgeBaseUserRepository.findByUserId(caller.id!!)
            }
            val spaceAdminKbIds =
                memberships.filter { it.role == KnowledgeBaseUserRole.ADMIN }.map { it.knowledgeBaseId }.toSet()

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
