package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.UpdateReplyUseCaseIn
import com.okestro.okchat.email.application.dto.UpdateReplyUseCaseOut
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class UpdateReplyUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: UpdateReplyUseCaseIn): UpdateReplyUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val pendingReply: PendingEmailReply = pendingEmailReplyRepository.findById(useCaseIn.id).orElse(null)
                ?: throw IllegalArgumentException("Pending reply not found: ${useCaseIn.id}")

            if (pendingReply.status != ReviewStatus.PENDING) {
                throw IllegalStateException("Can only update PENDING replies. Current status: ${pendingReply.status}")
            }

            val updated = pendingReply.copy(
                replyContent = useCaseIn.replyContent
            )
            pendingEmailReplyRepository.save(updated)
            logger.info { "Updated reply content: id=${useCaseIn.id}" }

            UpdateReplyUseCaseOut(true)
        }
}
