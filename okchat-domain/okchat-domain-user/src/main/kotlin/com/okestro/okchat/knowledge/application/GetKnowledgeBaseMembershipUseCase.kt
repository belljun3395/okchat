package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembershipUseCaseIn
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembershipUseCaseOut
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class GetKnowledgeBaseMembershipUseCase(
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository
) {
    suspend fun execute(useCaseIn: GetKnowledgeBaseMembershipUseCaseIn): GetKnowledgeBaseMembershipUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val membership =
                knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(useCaseIn.userId, useCaseIn.kbId)
            log.debug {
                "Get KB membership: kbId=${useCaseIn.kbId}, userId=${useCaseIn.userId}, found=${membership != null}"
            }
            GetKnowledgeBaseMembershipUseCaseOut(membership = membership)
        }
}
