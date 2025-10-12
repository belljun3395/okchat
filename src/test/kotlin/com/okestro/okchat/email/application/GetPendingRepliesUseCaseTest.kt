package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.GetPendingRepliesUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant

class GetPendingRepliesUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = GetPendingRepliesUseCase(pendingEmailReplyRepository)

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

    given("Multiple pending replies exist for the requested page") {
        val replies = listOf(createPendingReply(1), createPendingReply(2))
        val page: Page<PendingEmailReply> = PageImpl(replies, PageRequest.of(0, 2), 2)
        every { pendingEmailReplyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 2)) } returns page

        `when`("execute is called with explicit pagination") {
            val result = useCase.execute(GetPendingRepliesUseCaseIn(page = 0, size = 2))

            then("it should return the page from the service") {
                result.replies.content.shouldHaveSize(2)
                result.replies.totalElements shouldBe 2
                verify(exactly = 1) { pendingEmailReplyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 2)) }
            }
        }
    }

    given("Default pagination is used") {
        val page: Page<PendingEmailReply> = PageImpl(emptyList(), PageRequest.of(0, 20), 0)
        every { pendingEmailReplyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)) } returns page

        `when`("execute is called without arguments") {
            val result = useCase.execute(GetPendingRepliesUseCaseIn())

            then("it should request the default page from the service") {
                result.replies.content.shouldHaveSize(0)
                result.replies.totalElements shouldBe 0
                verify(exactly = 1) { pendingEmailReplyRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)) }
            }
        }
    }
})
