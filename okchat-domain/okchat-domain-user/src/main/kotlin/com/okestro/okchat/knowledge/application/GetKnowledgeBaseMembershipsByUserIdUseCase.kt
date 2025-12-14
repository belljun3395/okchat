package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembershipsByUserIdUseCaseIn
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembershipsByUserIdUseCaseOut
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class GetKnowledgeBaseMembershipsByUserIdUseCase(
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository
) {
    suspend fun execute(
        useCaseIn: GetKnowledgeBaseMembershipsByUserIdUseCaseIn
    ): GetKnowledgeBaseMembershipsByUserIdUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val memberships = knowledgeBaseUserRepository.findByUserId(useCaseIn.userId)
            log.debug { "Get KB memberships: userId=${useCaseIn.userId}, count=${memberships.size}" }
            GetKnowledgeBaseMembershipsByUserIdUseCaseOut(memberships = memberships)
        }
}
