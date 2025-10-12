package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingRepliesUseCaseIn
import com.okestro.okchat.email.application.dto.GetPendingRepliesUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class GetPendingRepliesUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    suspend fun execute(useCaseIn: GetPendingRepliesUseCaseIn): GetPendingRepliesUseCaseOut =
        withContext(Dispatchers.IO) {
            val pageRequest = PageRequest.of(useCaseIn.page, useCaseIn.size)
            val replies = pendingEmailReplyRepository.findAllByOrderByCreatedAtDesc(pageRequest)
            GetPendingRepliesUseCaseOut(replies)
        }
}
