package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingRepliesByStatusUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class GetPendingRepliesByStatusUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = GetPendingRepliesByStatusUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    fun createPendingReply(id: Long, status: ReviewStatus): PendingEmailReply = PendingEmailReply(
        id = id,
        fromEmail = "sender$id@example.com",
        toEmail = "receiver$id@example.com",
        originalSubject = "subject$id",
        originalContent = "content$id",
        replyContent = "reply$id",
        providerType = EmailProperties.EmailProviderType.GMAIL,
        messageId = "message-$id",
        status = status,
        createdAt = Instant.now()
    )

    given("Pending replies exist for a particular status") {
        val pendingReplies = listOf(createPendingReply(1, ReviewStatus.PENDING), createPendingReply(2, ReviewStatus.PENDING))
        every { pendingEmailReplyRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING) } returns pendingReplies

        `when`("execute is called with the status") {
            val result = useCase.execute(GetPendingRepliesByStatusUseCaseIn(ReviewStatus.PENDING))

            then("it should return all replies with that status") {
                result.replies.shouldHaveSize(2)
                result.replies.all { it.status == ReviewStatus.PENDING }.shouldBeTrue()
                verify(exactly = 1) { pendingEmailReplyRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING) }
            }
        }
    }

    given("No replies exist for the provided status") {
        every { pendingEmailReplyRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.REJECTED) } returns emptyList()

        `when`("execute is invoked for the rejected status") {
            val result = useCase.execute(GetPendingRepliesByStatusUseCaseIn(ReviewStatus.REJECTED))

            then("it should return an empty list") {
                result.replies.shouldBeEmpty()
                verify(exactly = 1) { pendingEmailReplyRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.REJECTED) }
            }
        }
    }
})
