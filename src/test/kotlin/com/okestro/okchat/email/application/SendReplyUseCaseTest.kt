package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.SendReplyUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import com.okestro.okchat.email.service.EmailReplyService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Optional

class SendReplyUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val emailReplyService = mockk<EmailReplyService>()
    val useCase = SendReplyUseCase(pendingEmailReplyRepository, emailReplyService)

    afterEach {
        clearAllMocks()
    }

    given("Send reply request") {
        val replyId = 1L
        val input = SendReplyUseCaseIn(replyId)

        val approvedReply = PendingEmailReply(
            id = replyId,
            fromEmail = "user@example.com",
            toEmail = "support@example.com",
            originalSubject = "Subject",
            originalContent = "Content",
            replyContent = "Final Reply",
            status = ReviewStatus.APPROVED,
            providerType = EmailProperties.EmailProviderType.GMAIL,
            createdAt = Instant.now(),
            reviewedAt = Instant.now(),
            reviewedBy = "admin@example.com"
        )

        `when`("Reply is in APPROVED status") {
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.of(approvedReply)
            coEvery { emailReplyService.sendReply(any(), any(), any()) } just Runs
            every { pendingEmailReplyRepository.save(any()) } answers { firstArg() }

            val result = useCase.execute(input)

            then("Email is sent and status updates to SENT") {
                result.result.isSuccess shouldBe true
                val sentReply = result.result.getOrNull()
                sentReply!!.status shouldBe ReviewStatus.SENT
                coVerify { emailReplyService.sendReply(any(), "Final Reply", EmailProperties.EmailProviderType.GMAIL) }
                verify { pendingEmailReplyRepository.save(match { it.status == ReviewStatus.SENT }) }
            }
        }

        `when`("Reply is NOT in APPROVED status") {
            val pendingReply = approvedReply.copy(status = ReviewStatus.PENDING)
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.of(pendingReply)

            val result = useCase.execute(input)

            then("Returns failure with IllegalStateException") {
                result.result.isFailure shouldBe true
                result.result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
                coVerify(exactly = 0) { emailReplyService.sendReply(any(), any(), any()) }
            }
        }

        `when`("Sending fails") {
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.of(approvedReply)
            coEvery { emailReplyService.sendReply(any(), any(), any()) } throws RuntimeException("SMTP Error")
            every { pendingEmailReplyRepository.save(any()) } answers { firstArg() }

            val result = useCase.execute(input)

            then("Status updates to FAILED and returns failure") {
                result.result.isFailure shouldBe true
                verify { pendingEmailReplyRepository.save(match { it.status == ReviewStatus.FAILED }) }
            }
        }
    }
})
