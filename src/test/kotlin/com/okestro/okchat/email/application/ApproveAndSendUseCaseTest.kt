package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.ApproveAndSendUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import com.okestro.okchat.email.service.EmailReplyService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.Optional

class ApproveAndSendUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val emailReplyService = mockk<EmailReplyService>()
    val useCase = ApproveAndSendUseCase(pendingEmailReplyRepository, emailReplyService)

    afterEach {
        clearAllMocks()
    }

    fun createPendingEmailReply(status: ReviewStatus): PendingEmailReply = PendingEmailReply(
        id = 1L,
        fromEmail = "sender@example.com",
        toEmail = "receiver@example.com",
        originalSubject = "subject",
        originalContent = "content",
        replyContent = "reply",
        providerType = EmailProperties.EmailProviderType.GMAIL,
        messageId = "message-id",
        status = status,
        createdAt = Instant.now()
    )

    given("A pending email reply is approved and sent successfully") {
        val pendingReply = createPendingEmailReply(ReviewStatus.PENDING)

        every { pendingEmailReplyRepository.findById(1L) } returns Optional.of(pendingReply)
        every { pendingEmailReplyRepository.save(any()) } answers { firstArg() }
        coEvery { emailReplyService.sendReply(any(), any(), any()) } returns Unit

        `when`("execute is called") {
            val output = runBlocking { useCase.execute(ApproveAndSendUseCaseIn(1L, "reviewer")) }

            then("it should return a successful result with a sent reply") {
                output.result.isSuccess.shouldBeTrue()
                output.result.getOrNull()?.status shouldBe ReviewStatus.SENT
                verify(exactly = 1) { pendingEmailReplyRepository.findById(1L) }
                verify(atLeast = 2) { pendingEmailReplyRepository.save(any()) }
                coVerify(exactly = 1) { emailReplyService.sendReply(any(), any(), any()) }
            }
        }
    }

    given("Approving and sending a reply fails downstream") {
        val pendingReply = createPendingEmailReply(ReviewStatus.PENDING)

        every { pendingEmailReplyRepository.findById(2L) } returns Optional.of(pendingReply)
        every { pendingEmailReplyRepository.save(any()) } answers { firstArg() }
        coEvery { emailReplyService.sendReply(any(), any(), any()) } throws RuntimeException("Cannot send")

        `when`("execute is invoked") {
            val output = runBlocking { useCase.execute(ApproveAndSendUseCaseIn(2L, "reviewer")) }

            then("it should capture the failure and mark the reply as failed") {
                output.result.isSuccess shouldBe false
                verify(exactly = 1) { pendingEmailReplyRepository.findById(2L) }
                verify(atLeast = 2) { pendingEmailReplyRepository.save(any()) }
                coVerify(exactly = 1) { emailReplyService.sendReply(any(), any(), any()) }
            }
        }
    }

    given("The pending reply does not exist") {
        every { pendingEmailReplyRepository.findById(99L) } returns Optional.empty()

        `when`("execute is called") {
            val output = runBlocking { useCase.execute(ApproveAndSendUseCaseIn(99L, "reviewer")) }

            then("it should return a failure result") {
                output.result.isSuccess shouldBe false
                verify(exactly = 1) { pendingEmailReplyRepository.findById(99L) }
            }
        }
    }

    given("The pending reply is not in PENDING status") {
        val sentReply = createPendingEmailReply(ReviewStatus.SENT)
        every { pendingEmailReplyRepository.findById(3L) } returns Optional.of(sentReply)

        `when`("execute is called") {
            val output = runBlocking { useCase.execute(ApproveAndSendUseCaseIn(3L, "reviewer")) }

            then("it should return a failure indicating invalid status") {
                output.result.isSuccess shouldBe false
                verify(exactly = 1) { pendingEmailReplyRepository.findById(3L) }
                verify(exactly = 0) { pendingEmailReplyRepository.save(any()) }
                coVerify(exactly = 0) { emailReplyService.sendReply(any(), any(), any()) }
            }
        }
    }
})
