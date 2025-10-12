package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingReplyByIdUseCaseIn
import com.okestro.okchat.email.application.dto.GetPendingReplyByIdUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import org.springframework.stereotype.Service

@Service
class GetPendingReplyByIdUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    fun execute(useCaseIn: GetPendingReplyByIdUseCaseIn): GetPendingReplyByIdUseCaseOut {
        val reply = pendingEmailReplyRepository.findById(useCaseIn.id).orElse(null)
        return GetPendingReplyByIdUseCaseOut(reply)
    }
}
