package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.DeletePendingReplyUseCaseIn
import com.okestro.okchat.email.application.dto.DeletePendingReplyUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class DeletePendingReplyUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: DeletePendingReplyUseCaseIn): DeletePendingReplyUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            pendingEmailReplyRepository.deleteById(useCaseIn.id)
            logger.info { "Deleted pending email reply: id=${useCaseIn.id}" }
            DeletePendingReplyUseCaseOut(true)
        }
}
