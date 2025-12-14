package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingRepliesByStatusUseCaseIn
import com.okestro.okchat.email.application.dto.GetPendingRepliesByStatusUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetPendingRepliesByStatusUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    suspend fun execute(useCaseIn: GetPendingRepliesByStatusUseCaseIn): GetPendingRepliesByStatusUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val replies = pendingEmailReplyRepository.findByStatusOrderByCreatedAtDesc(useCaseIn.status)
            GetPendingRepliesByStatusUseCaseOut(replies)
        }
}
