package com.okestro.okchat.email.service

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.provider.EmailMessage
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Date

private val logger = KotlinLogging.logger {}

/**
 * Service for managing pending email replies
 * Handles creating, reviewing, and sending email replies
 */
@Service
class PendingEmailReplyService(
    private val repository: PendingEmailReplyRepository,
    private val emailReplyService: EmailReplyService
) {

    /**
     * Save a pending email reply for review
     */
    @Transactional("transactionManager")
    suspend fun savePendingReply(
        originalMessage: EmailMessage,
        replyContent: String,
        providerType: EmailProperties.EmailProviderType,
        toEmail: String
    ): PendingEmailReply = withContext(Dispatchers.IO) {
        val messageId = originalMessage.rawMessage.getHeader("Message-ID")?.firstOrNull()

        val pendingReply = PendingEmailReply(
            fromEmail = originalMessage.from,
            toEmail = toEmail,
            originalSubject = originalMessage.subject,
            originalContent = originalMessage.content.take(2000), // Truncate for storage
            replyContent = replyContent,
            providerType = providerType,
            messageId = messageId,
            status = ReviewStatus.PENDING
        )

        val saved = repository.save(pendingReply)
        logger.info { "Saved pending email reply: id=${saved.id}, from=${saved.fromEmail}, subject=${saved.originalSubject}" }
        saved
    }

    /**
     * Get all pending replies with pagination
     */
    fun getPendingReplies(page: Int = 0, size: Int = 20): Page<PendingEmailReply> {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
    }

    /**
     * Get pending replies by status
     */
    fun getPendingRepliesByStatus(status: ReviewStatus): List<PendingEmailReply> {
        return repository.findByStatusOrderByCreatedAtDesc(status)
    }

    /**
     * Get a specific pending reply by ID
     */
    fun getPendingReplyById(id: Long): PendingEmailReply? {
        return repository.findById(id).orElse(null)
    }

    /**
     * Count pending replies by status
     */
    fun countByStatus(status: ReviewStatus): Long {
        return repository.countByStatus(status)
    }

    /**
     * Approve and send an email reply
     */
    @Transactional("transactionManager")
    suspend fun approveAndSend(
        id: Long,
        reviewedBy: String
    ): Result<PendingEmailReply> = withContext(Dispatchers.IO) {
        try {
            val pendingReply = repository.findById(id).orElse(null)
                ?: return@withContext Result.failure(IllegalArgumentException("Pending reply not found: $id"))

            if (pendingReply.status != ReviewStatus.PENDING) {
                return@withContext Result.failure(IllegalStateException("Reply is not in PENDING status: ${pendingReply.status}"))
            }

            // Update status to APPROVED first
            val approved = pendingReply.copy(
                status = ReviewStatus.APPROVED,
                reviewedAt = Instant.now(),
                reviewedBy = reviewedBy
            )
            repository.save(approved)

            logger.info { "Approved email reply: id=$id, from=${pendingReply.fromEmail}" }

            // Try to send the email
            try {
                // Reconstruct EmailMessage for sending
                val emailMessage = reconstructEmailMessage(pendingReply)
                emailReplyService.sendReply(
                    originalMessage = emailMessage,
                    replyContent = pendingReply.replyContent,
                    providerType = pendingReply.providerType
                )

                // Update status to SENT
                val sent = approved.copy(
                    status = ReviewStatus.SENT,
                    sentAt = Instant.now()
                )
                val saved = repository.save(sent)
                logger.info { "Successfully sent email reply: id=$id" }
                Result.success(saved)
            } catch (e: Exception) {
                logger.error(e) { "Failed to send approved email: id=$id" }
                // Update status to FAILED
                val failed = approved.copy(status = ReviewStatus.FAILED)
                repository.save(failed)
                Result.failure(e)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error approving email reply: id=$id" }
            Result.failure(e)
        }
    }

    /**
     * Reject an email reply
     */
    @Transactional("transactionManager")
    suspend fun reject(
        id: Long,
        reviewedBy: String,
        rejectionReason: String?
    ): Result<PendingEmailReply> = withContext(Dispatchers.IO) {
        try {
            val pendingReply = repository.findById(id).orElse(null)
                ?: return@withContext Result.failure(IllegalArgumentException("Pending reply not found: $id"))

            if (pendingReply.status != ReviewStatus.PENDING) {
                return@withContext Result.failure(IllegalStateException("Reply is not in PENDING status: ${pendingReply.status}"))
            }

            val rejected = pendingReply.copy(
                status = ReviewStatus.REJECTED,
                reviewedAt = Instant.now(),
                reviewedBy = reviewedBy,
                rejectionReason = rejectionReason
            )
            val saved = repository.save(rejected)
            logger.info { "Rejected email reply: id=$id, reason=$rejectionReason" }
            Result.success(saved)
        } catch (e: Exception) {
            logger.error(e) { "Error rejecting email reply: id=$id" }
            Result.failure(e)
        }
    }

    /**
     * Reconstruct EmailMessage from PendingEmailReply for sending
     * This is a simplified reconstruction - we only need the essential fields for replying
     */
    private fun reconstructEmailMessage(pendingReply: PendingEmailReply): EmailMessage {
        // Create a mock EmailMessage with the essential information
        // The rawMessage is used for getting Message-ID for threading
        val session = jakarta.mail.Session.getDefaultInstance(java.util.Properties())
        val mimeMessage = jakarta.mail.internet.MimeMessage(session)

        // Set Message-ID if available
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

    /**
     * Delete a pending reply (for cleanup)
     */
    @Transactional("transactionManager")
    fun delete(id: Long) {
        repository.deleteById(id)
        logger.info { "Deleted pending email reply: id=$id" }
    }
}
