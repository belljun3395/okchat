package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.ApproveReplyUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Optional

class ApproveReplyUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = ApproveReplyUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    given("Approve reply request") {
        val replyId = 1L
        val reviewer = "admin@example.com"
        val input = ApproveReplyUseCaseIn(replyId, reviewer)

        val pendingReply = PendingEmailReply(
            id = replyId,
            fromEmail = "user@example.com",
            toEmail = "support@example.com",
            originalSubject = "Subject",
            originalContent = "Content",
            replyContent = "Draft Reply",
            status = ReviewStatus.PENDING,
            providerType = EmailProperties.EmailProviderType.GMAIL,
            createdAt = Instant.now()
        )

        `when`("Reply is in PENDING status") {
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.of(pendingReply)
            every { pendingEmailReplyRepository.save(any()) } answers { firstArg() }

            val result = useCase.execute(input)

            then("Status updates to APPROVED") {
                result.result.isSuccess shouldBe true
                val approvedReply = result.result.getOrNull()
                approvedReply!!.status shouldBe ReviewStatus.APPROVED
                approvedReply.reviewedBy shouldBe reviewer
                verify { pendingEmailReplyRepository.save(any()) }
            }
        }

        `when`("Reply is NOT in PENDING status") {
            val approvedReply = pendingReply.copy(status = ReviewStatus.APPROVED)
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.of(approvedReply)

            val result = useCase.execute(input)

            then("Returns failure with IllegalStateException") {
                result.result.isFailure shouldBe true
                result.result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
            }
        }

        `when`("Reply does not exist") {
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.empty()

            val result = useCase.execute(input)

            then("Returns failure with IllegalArgumentException") {
                result.result.isFailure shouldBe true
                result.result.exceptionOrNull() shouldBe instanceOf<IllegalArgumentException>()
            }
        }
    }
})
