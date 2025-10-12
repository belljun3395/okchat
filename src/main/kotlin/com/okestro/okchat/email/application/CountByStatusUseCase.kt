package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.CountByStatusUseCaseIn
import com.okestro.okchat.email.application.dto.CountByStatusUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class CountByStatusUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    suspend fun execute(useCaseIn: CountByStatusUseCaseIn): CountByStatusUseCaseOut =
        withContext(Dispatchers.IO) {
            val count = pendingEmailReplyRepository.countByStatus(useCaseIn.status)
            CountByStatusUseCaseOut(count)
        }
}
