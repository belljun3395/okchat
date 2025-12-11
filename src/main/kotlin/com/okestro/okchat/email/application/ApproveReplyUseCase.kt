package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.ApproveReplyUseCaseIn
import com.okestro.okchat.email.application.dto.ApproveReplyUseCaseOut
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class ApproveReplyUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: ApproveReplyUseCaseIn): ApproveReplyUseCaseOut =
        withContext(Dispatchers.IO) {
            ApproveReplyUseCaseOut(
                approveInternal(useCaseIn.id, useCaseIn.reviewedBy)
            )
        }

    private suspend fun approveInternal(
        id: Long,
        reviewedBy: String
    ): Result<PendingEmailReply> {
        val pendingReply: PendingEmailReply = pendingEmailReplyRepository.findById(id).orElse(null)
            ?: return Result.failure(IllegalArgumentException("Pending reply not found: $id"))

        if (pendingReply.status != ReviewStatus.PENDING) {
            return Result.failure(
                IllegalStateException("Reply is not in PENDING status: ${pendingReply.status}")
            )
        }

        val approved = pendingReply.copy(
            status = ReviewStatus.APPROVED,
            reviewedAt = Instant.now(),
            reviewedBy = reviewedBy
        )
        val saved = pendingEmailReplyRepository.save(approved)
        logger.info { "Approved email reply: id=$id, from=${pendingReply.fromEmail}" }

        return Result.success(saved)
    }
}
