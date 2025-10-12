package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingRepliesByStatusUseCaseIn
import com.okestro.okchat.email.application.dto.GetPendingRepliesByStatusUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import org.springframework.stereotype.Service

@Service
class GetPendingRepliesByStatusUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    fun execute(useCaseIn: GetPendingRepliesByStatusUseCaseIn): GetPendingRepliesByStatusUseCaseOut {
        val replies = pendingEmailReplyRepository.findByStatusOrderByCreatedAtDesc(useCaseIn.status)
        return GetPendingRepliesByStatusUseCaseOut(replies)
    }
}
