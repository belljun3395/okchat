package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingReplyByIdUseCaseIn
import com.okestro.okchat.email.application.dto.GetPendingReplyByIdUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetPendingReplyByIdUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    suspend fun execute(useCaseIn: GetPendingReplyByIdUseCaseIn): GetPendingReplyByIdUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val reply = pendingEmailReplyRepository.findById(useCaseIn.id).orElse(null)
            GetPendingReplyByIdUseCaseOut(reply)
        }
}
