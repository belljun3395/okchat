package com.okestro.okchat.email.service

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.provider.EmailMessage
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.*

@DisplayName("PendingEmailReplyService Unit Tests")
class PendingEmailReplyServiceTest {

    private lateinit var repository: PendingEmailReplyRepository
    private lateinit var emailReplyService: EmailReplyService
    private lateinit var service: PendingEmailReplyService

    @BeforeEach
    fun setUp() {
        repository = mockk()
        emailReplyService = mockk()
        service = PendingEmailReplyService(repository, emailReplyService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("savePendingReply should save pending reply")
    fun `should save pending reply`() = runTest {
        // given
        val originalMessage = createEmailMessage()
        val replyContent = "답장 내용입니다"
        val providerType = EmailProperties.EmailProviderType.GMAIL
        val toEmail = "reply@example.com"

        val savedReply = PendingEmailReply(
            id = 1L,
            fromEmail = "sender@example.com",
            toEmail = toEmail,
            originalSubject = "테스트",
            originalContent = "원본 내용",
            replyContent = replyContent,
            providerType = providerType,
            messageId = "msg-123",
            status = ReviewStatus.PENDING,
            createdAt = Instant.now()
        )

        every { repository.save(any()) } returns savedReply

        // when
        val result = service.savePendingReply(originalMessage, replyContent, providerType, toEmail)

        // then
        result.shouldNotBeNull()
        result.id shouldBe 1L
        result.fromEmail shouldBe "sender@example.com"
        result.replyContent shouldBe replyContent
        result.status shouldBe ReviewStatus.PENDING

        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    @DisplayName("getPendingReplies should get pending replies with pagination")
    fun `should get pending replies with pagination`() {
        // given
        val replies = listOf(
            createPendingReply(1L),
            createPendingReply(2L)
        )
        val page: Page<PendingEmailReply> = PageImpl(replies, PageRequest.of(0, 20), 2)
        every { repository.findAllByOrderByCreatedAtDesc(any()) } returns page

        // when
        val result = service.getPendingReplies(0, 20)

        // then
        result.content.size shouldBe 2
        result.totalElements shouldBe 2
    }

    @Test
    @DisplayName("getPendingRepliesByStatus should get pending replies by status")
    fun `should get pending replies by status`() {
        // given
        val replies = listOf(createPendingReply(1L, ReviewStatus.PENDING))
        every { repository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING) } returns replies

        // when
        val result = service.getPendingRepliesByStatus(ReviewStatus.PENDING)

        // then
        result.size shouldBe 1
        result[0].status shouldBe ReviewStatus.PENDING
    }

    @Test
    @DisplayName("getPendingReplyById should get pending reply by id")
    fun `should get pending reply by id`() {
        // given
        val reply = createPendingReply(1L)
        every { repository.findById(1L) } returns Optional.of(reply)

        // when
        val result = service.getPendingReplyById(1L)

        // then
        result.shouldNotBeNull()
        result.id shouldBe 1L
    }

    @Test
    @DisplayName("getPendingReplyById should return null when reply not found")
    fun `should return null when reply not found`() {
        // given
        every { repository.findById(999L) } returns Optional.empty()

        // when
        val result = service.getPendingReplyById(999L)

        // then
        result.shouldBeNull()
    }

    @Test
    @DisplayName("countByStatus should count by status")
    fun `should count by status`() {
        // given
        every { repository.countByStatus(ReviewStatus.PENDING) } returns 5L

        // when
        val result = service.countByStatus(ReviewStatus.PENDING)

        // then
        result shouldBe 5L
    }

    @Test
    @DisplayName("approveAndSend should approve and send reply successfully")
    fun `should approve and send reply successfully`() = runTest {
        // given
        val pendingReply = createPendingReply(1L, ReviewStatus.PENDING)
        val approvedReply = pendingReply.copy(
            status = ReviewStatus.APPROVED,
            reviewedAt = Instant.now(),
            reviewedBy = "reviewer"
        )
        val sentReply = approvedReply.copy(
            status = ReviewStatus.SENT,
            sentAt = Instant.now()
        )

        every { repository.findById(1L) } returns Optional.of(pendingReply)
        every { repository.save(match { it.status == ReviewStatus.APPROVED }) } returns approvedReply
        every { repository.save(match { it.status == ReviewStatus.SENT }) } returns sentReply
        coEvery { emailReplyService.sendReply(any(), any(), any()) } just Runs

        // when
        val result = service.approveAndSend(1L, "reviewer")

        // then
        result.isSuccess shouldBe true
        result.getOrNull()?.status shouldBe ReviewStatus.SENT

        verify(exactly = 2) { repository.save(any()) }
        coVerify(exactly = 1) { emailReplyService.sendReply(any(), any(), any()) }
    }

    @Test
    @DisplayName("approveAndSend should set FAILED status when send fails")
    fun `should set FAILED status when send fails`() = runTest {
        // given
        val pendingReply = createPendingReply(1L, ReviewStatus.PENDING)
        val approvedReply = pendingReply.copy(status = ReviewStatus.APPROVED)
        val failedReply = approvedReply.copy(status = ReviewStatus.FAILED)

        every { repository.findById(1L) } returns Optional.of(pendingReply)
        every { repository.save(match { it.status == ReviewStatus.APPROVED }) } returns approvedReply
        every { repository.save(match { it.status == ReviewStatus.FAILED }) } returns failedReply
        coEvery { emailReplyService.sendReply(any(), any(), any()) } throws RuntimeException("Send failed")

        // when
        val result = service.approveAndSend(1L, "reviewer")

        // then
        result.isFailure shouldBe true
        verify(exactly = 2) { repository.save(any()) }
    }

    @Test
    @DisplayName("approveAndSend should fail when reply not found")
    fun `should fail when reply not found for approval`() = runTest {
        // given
        every { repository.findById(999L) } returns Optional.empty()

        // when
        val result = service.approveAndSend(999L, "reviewer")

        // then
        result.isFailure shouldBe true
    }

    @Test
    @DisplayName("approveAndSend should fail when reply is not in PENDING status")
    fun `should fail when reply is not in PENDING status`() = runTest {
        // given
        val sentReply = createPendingReply(1L, ReviewStatus.SENT)
        every { repository.findById(1L) } returns Optional.of(sentReply)

        // when
        val result = service.approveAndSend(1L, "reviewer")

        // then
        result.isFailure shouldBe true
    }

    @Test
    @DisplayName("reject should reject reply successfully")
    fun `should reject reply successfully`() = runTest {
        // given
        val pendingReply = createPendingReply(1L, ReviewStatus.PENDING)
        val rejectedReply = pendingReply.copy(
            status = ReviewStatus.REJECTED,
            reviewedAt = Instant.now(),
            reviewedBy = "reviewer",
            rejectionReason = "Not appropriate"
        )

        every { repository.findById(1L) } returns Optional.of(pendingReply)
        every { repository.save(any()) } returns rejectedReply

        // when
        val result = service.reject(1L, "reviewer", "Not appropriate")

        // then
        result.isSuccess shouldBe true
        result.getOrNull()?.status shouldBe ReviewStatus.REJECTED
        result.getOrNull()?.rejectionReason shouldBe "Not appropriate"
    }

    @Test
    @DisplayName("delete should delete reply")
    fun `should delete reply`() {
        // given
        every { repository.deleteById(1L) } just Runs

        // when
        service.delete(1L)

        // then
        verify(exactly = 1) { repository.deleteById(1L) }
    }

    private fun createEmailMessage(): EmailMessage {
        val session = Session.getDefaultInstance(Properties())
        val mimeMessage = MimeMessage(session)
        mimeMessage.setHeader("Message-ID", "msg-123")

        return EmailMessage(
            id = "email-1",
            from = "sender@example.com",
            to = listOf("recipient@example.com"),
            subject = "테스트",
            content = "원본 내용",
            receivedDate = Date(),
            rawMessage = mimeMessage
        )
    }

    private fun createPendingReply(
        id: Long,
        status: ReviewStatus = ReviewStatus.PENDING
    ): PendingEmailReply {
        return PendingEmailReply(
            id = id,
            fromEmail = "sender@example.com",
            toEmail = "reply@example.com",
            originalSubject = "테스트",
            originalContent = "원본 내용",
            replyContent = "답장 내용",
            providerType = EmailProperties.EmailProviderType.GMAIL,
            messageId = "msg-$id",
            status = status,
            createdAt = Instant.now()
        )
    }
}
