package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.DeletePendingReplyUseCaseIn
import com.okestro.okchat.email.application.dto.DeletePendingReplyUseCaseOut
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class DeletePendingReplyUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    @Transactional("transactionManager")
    fun execute(useCaseIn: DeletePendingReplyUseCaseIn): DeletePendingReplyUseCaseOut {
        pendingEmailReplyRepository.deleteById(useCaseIn.id)
        logger.info { "Deleted pending email reply: id=${useCaseIn.id}" }
        return DeletePendingReplyUseCaseOut(true)
    }
}
