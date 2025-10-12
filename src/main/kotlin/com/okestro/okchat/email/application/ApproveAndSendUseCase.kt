package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.ApproveAndSendUseCaseIn
import com.okestro.okchat.email.application.dto.ApproveAndSendUseCaseOut
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.provider.EmailMessage
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import com.okestro.okchat.email.service.EmailReplyService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Date

private val logger = KotlinLogging.logger {}

@Service
class ApproveAndSendUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository,
    private val emailReplyService: EmailReplyService
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: ApproveAndSendUseCaseIn): ApproveAndSendUseCaseOut =
        withContext(Dispatchers.IO) {
            ApproveAndSendUseCaseOut(
                approveAndSendInternal(useCaseIn.id, useCaseIn.reviewedBy)
            )
        }

    private suspend fun approveAndSendInternal(
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
        pendingEmailReplyRepository.save(approved)
        logger.info { "Approved email reply: id=$id, from=${pendingReply.fromEmail}" }

        return try {
            val emailMessage = reconstructEmailMessage(pendingReply)
            emailReplyService.sendReply(
                originalMessage = emailMessage,
                replyContent = pendingReply.replyContent,
                providerType = pendingReply.providerType
            )

            val sent = approved.copy(
                status = ReviewStatus.SENT,
                sentAt = Instant.now()
            )
            val saved = pendingEmailReplyRepository.save(sent)
            logger.info { "Successfully sent email reply: id=$id" }
            Result.success(saved)
        } catch (e: Exception) {
            logger.error(e) { "Failed to send approved email: id=$id" }
            val failed = approved.copy(status = ReviewStatus.FAILED)
            pendingEmailReplyRepository.save(failed)
            Result.failure(e)
        }
    }

    private fun reconstructEmailMessage(pendingReply: PendingEmailReply): EmailMessage {
        val session = Session.getDefaultInstance(java.util.Properties())
        val mimeMessage = MimeMessage(session)
        if (pendingReply.messageId != null) {
            mimeMessage.setHeader("Message-ID", pendingReply.messageId)
        }
        return EmailMessage(
            id = pendingReply.id?.toString() ?: "pending-${System.currentTimeMillis()}",
            from = pendingReply.fromEmail,
            to = listOf(pendingReply.toEmail),
            subject = pendingReply.originalSubject,
            content = pendingReply.originalContent,
            receivedDate = Date.from(pendingReply.createdAt),
            rawMessage = mimeMessage
        )
    }
}
