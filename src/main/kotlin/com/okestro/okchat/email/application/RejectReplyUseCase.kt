package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.RejectReplyUseCaseIn
import com.okestro.okchat.email.application.dto.RejectReplyUseCaseOut
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
class RejectReplyUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: RejectReplyUseCaseIn): RejectReplyUseCaseOut = withContext(Dispatchers.IO) {
        val result = rejectInternal(
            id = useCaseIn.id,
            reviewedBy = useCaseIn.reviewedBy,
            rejectionReason = useCaseIn.rejectionReason
        )
        RejectReplyUseCaseOut(result)
    }

    private fun rejectInternal(
        id: Long,
        reviewedBy: String,
        rejectionReason: String?
    ): Result<PendingEmailReply> {
        val pendingReply = pendingEmailReplyRepository.findById(id).orElse(null)
            ?: return Result.failure(IllegalArgumentException("Pending reply not found: $id"))

        if (pendingReply.status != ReviewStatus.PENDING) {
            return Result.failure(
                IllegalStateException("Reply is not in PENDING status: ${pendingReply.status}")
            )
        }

        val rejected = pendingReply.copy(
            status = ReviewStatus.REJECTED,
            reviewedAt = Instant.now(),
            reviewedBy = reviewedBy,
            rejectionReason = rejectionReason
        )
        val saved = pendingEmailReplyRepository.save(rejected)
        logger.info { "Rejected email reply: id=$id, reason=$rejectionReason" }
        return Result.success(saved)
    }
}
