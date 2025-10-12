package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.RejectReplyUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.Optional

class RejectReplyUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = RejectReplyUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    fun createPendingReply(status: ReviewStatus = ReviewStatus.PENDING): PendingEmailReply = PendingEmailReply(
        id = 10L,
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

    given("A reviewer rejects an email reply with a justification") {
        val pending = createPendingReply()
        every { pendingEmailReplyRepository.findById(10L) } returns Optional.of(pending)
        every { pendingEmailReplyRepository.save(any()) } answers { firstArg() }

        `when`("execute is called") {
            val output = runBlocking {
                useCase.execute(
                    RejectReplyUseCaseIn(
                        id = 10L,
                        reviewedBy = "reviewer",
                        rejectionReason = "Not appropriate"
                    )
                )
            }
            then("the use case should return the rejected reply") {
                output.result.isSuccess.shouldBeTrue()
                output.result.getOrNull()?.status shouldBe ReviewStatus.REJECTED
                verify(exactly = 1) { pendingEmailReplyRepository.findById(10L) }
                verify(exactly = 1) { pendingEmailReplyRepository.save(any()) }
            }
        }
    }

    given("Rejecting a reply fails because it no longer exists") {
        every { pendingEmailReplyRepository.findById(99L) } returns Optional.empty()

        `when`("execute is invoked without a reason") {
            val output = runBlocking {
                useCase.execute(
                    RejectReplyUseCaseIn(
                        id = 99L,
                        reviewedBy = "reviewer",
                        rejectionReason = null
                    )
                )
            }
            then("it should forward the failure") {
                output.result.isSuccess.shouldBeFalse()
                verify(exactly = 1) { pendingEmailReplyRepository.findById(99L) }
                verify(exactly = 0) { pendingEmailReplyRepository.save(any()) }
            }
        }
    }

    given("Rejecting a reply fails because status is not PENDING") {
        val nonPending = createPendingReply(ReviewStatus.SENT)
        every { pendingEmailReplyRepository.findById(11L) } returns Optional.of(nonPending)

        `when`("execute is invoked") {
            val output = runBlocking {
                useCase.execute(
                    RejectReplyUseCaseIn(
                        id = 11L,
                        reviewedBy = "reviewer",
                        rejectionReason = "reason"
                    )
                )
            }
            then("it should return a failure without saving") {
                output.result.isSuccess.shouldBeFalse()
                verify(exactly = 1) { pendingEmailReplyRepository.findById(11L) }
                verify(exactly = 0) { pendingEmailReplyRepository.save(any()) }
            }
        }
    }
})
