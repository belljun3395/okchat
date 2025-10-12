package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingRepliesUseCaseIn
import com.okestro.okchat.email.application.dto.GetPendingRepliesUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class GetPendingRepliesUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    fun execute(useCaseIn: GetPendingRepliesUseCaseIn): GetPendingRepliesUseCaseOut {
        val pageRequest = PageRequest.of(useCaseIn.page, useCaseIn.size)
        val replies = pendingEmailReplyRepository.findAllByOrderByCreatedAtDesc(pageRequest)
        return GetPendingRepliesUseCaseOut(replies)
    }
}
