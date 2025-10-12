package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingReplyByIdUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class GetPendingReplyByIdUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = GetPendingReplyByIdUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    fun createPendingReply(id: Long): PendingEmailReply = PendingEmailReply(
        id = id,
        fromEmail = "sender$id@example.com",
        toEmail = "receiver$id@example.com",
        originalSubject = "subject$id",
        originalContent = "content$id",
        replyContent = "reply$id",
        providerType = EmailProperties.EmailProviderType.GMAIL,
        messageId = "message-$id",
        status = ReviewStatus.PENDING,
        createdAt = Instant.now()
    )

    given("A pending reply exists for a particular id") {
        val reply = createPendingReply(5L)
        every { pendingEmailReplyRepository.findById(5L) } returns java.util.Optional.of(reply)

        `when`("execute is called with the id") {
            val result = useCase.execute(GetPendingReplyByIdUseCaseIn(5L))

            then("it should return the reply") {
                result.reply shouldBe reply
                verify(exactly = 1) { pendingEmailReplyRepository.findById(5L) }
            }
        }
    }

    given("No pending reply exists for the requested id") {
        every { pendingEmailReplyRepository.findById(6L) } returns java.util.Optional.empty()

        `when`("the use case runs") {
            val result = useCase.execute(GetPendingReplyByIdUseCaseIn(6L))

            then("the reply should be null") {
                result.reply.shouldBeNull()
                verify(exactly = 1) { pendingEmailReplyRepository.findById(6L) }
            }
        }
    }
})
