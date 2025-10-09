package com.okestro.okchat.email.model

import com.okestro.okchat.email.config.EmailProperties
import jakarta.persistence.*
import java.time.Instant

/**
 * Entity for storing email replies pending review before sending
 */
@Entity
@Table(
    name = "pending_email_replies",
    indexes = [
        Index(name = "idx_pending_email_status", columnList = "status"),
        Index(name = "idx_pending_email_created", columnList = "created_at"),
        Index(name = "idx_pending_email_from", columnList = "from_email")
    ]
)
data class PendingEmailReply(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * Email address of the original sender (who will receive the reply)
     */
    @Column(name = "from_email", nullable = false, length = 255)
    val fromEmail: String,

    /**
     * Email address that received the question (our system email)
     */
    @Column(name = "to_email", nullable = false, length = 255)
    val toEmail: String,

    /**
     * Original email subject
     */
    @Column(name = "original_subject", nullable = false, length = 500)
    val originalSubject: String,

    /**
     * Original email content (truncated)
     */
    @Column(name = "original_content", columnDefinition = "TEXT")
    val originalContent: String,

    /**
     * AI-generated reply content
     */
    @Column(name = "reply_content", nullable = false, columnDefinition = "TEXT")
    val replyContent: String,

    /**
     * Email provider type (GMAIL or OUTLOOK)
     */
    @Column(name = "provider_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val providerType: EmailProperties.EmailProviderType,

    /**
     * Original message ID for threading
     */
    @Column(name = "message_id", length = 500)
    val messageId: String? = null,

    /**
     * Review status
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: ReviewStatus = ReviewStatus.PENDING,

    /**
     * When the reply was created
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    /**
     * When the reply was reviewed (approved or rejected)
     */
    @Column(name = "reviewed_at")
    val reviewedAt: Instant? = null,

    /**
     * Who reviewed the reply (email or user ID)
     */
    @Column(name = "reviewed_by", length = 255)
    val reviewedBy: String? = null,

    /**
     * When the reply was actually sent
     */
    @Column(name = "sent_at")
    val sentAt: Instant? = null,

    /**
     * Rejection reason if status is REJECTED
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    val rejectionReason: String? = null
)

/**
 * Review status for pending email replies
 */
enum class ReviewStatus {
    /** Waiting for review */
    PENDING,

    /** Approved and ready to send */
    APPROVED,

    /** Rejected, will not be sent */
    REJECTED,

    /** Successfully sent */
    SENT,

    /** Failed to send after approval */
    FAILED
}
