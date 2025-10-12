package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.CountByStatusUseCaseIn
import com.okestro.okchat.email.application.dto.CountByStatusUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import org.springframework.stereotype.Service

@Service
class CountByStatusUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    fun execute(useCaseIn: CountByStatusUseCaseIn): CountByStatusUseCaseOut {
        val count = pendingEmailReplyRepository.countByStatus(useCaseIn.status)
        return CountByStatusUseCaseOut(count)
    }
}
