package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.UpdateReplyUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Optional

class UpdateReplyUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = UpdateReplyUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    given("Update reply content request") {
        val replyId = 1L
        val newContent = "Updated Content"
        val input = UpdateReplyUseCaseIn(replyId, newContent)

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

            then("Content is updated") {
                result.success shouldBe true
                verify { pendingEmailReplyRepository.save(match { it.replyContent == newContent }) }
            }
        }

        `when`("Reply is NOT in PENDING status") {
            val approvedReply = pendingReply.copy(status = ReviewStatus.APPROVED)
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.of(approvedReply)

            then("IllegalStateException is thrown") {
                shouldThrow<IllegalStateException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("Reply does not exist") {
            every { pendingEmailReplyRepository.findById(replyId) } returns Optional.empty()

            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
