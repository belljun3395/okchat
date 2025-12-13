package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.SavePendingReplyUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.provider.EmailMessage
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.Properties

class SavePendingReplyUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = SavePendingReplyUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    fun createEmailMessage(): EmailMessage {
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

    given("A generated reply should be persisted for review") {
        val message = createEmailMessage()
        val pendingSlot = slot<PendingEmailReply>()

        every { pendingEmailReplyRepository.save(capture(pendingSlot)) } answers {
            pendingSlot.captured.copy(id = 42L)
        }

        `when`("the save use case executes") {
            val output = runBlocking {
                useCase.execute(
                    SavePendingReplyUseCaseIn(
                        originalMessage = message,
                        replyContent = "답변 내용",
                        providerType = EmailProperties.EmailProviderType.GMAIL,
                        toEmail = "reply@example.com",
                        knowledgeBaseId = 0L
                    )
                )
            }
            then("it should persist the pending reply and return the saved instance") {
                output.pendingReply.id shouldBe 42L
                output.pendingReply.fromEmail shouldBe "sender@example.com"
                output.pendingReply.toEmail shouldBe "reply@example.com"
                output.pendingReply.replyContent shouldBe "답변 내용"
                verify(exactly = 1) { pendingEmailReplyRepository.save(any()) }
            }
        }
    }
})
