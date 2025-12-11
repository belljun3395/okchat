package com.okestro.okchat.email.controller

import com.okestro.okchat.email.application.SavePendingReplyUseCase
import com.okestro.okchat.email.application.dto.SavePendingReplyUseCaseIn
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.provider.EmailMessage
import io.swagger.v3.oas.annotations.Hidden
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/dev")
@Hidden
class DevTestingController(
    private val savePendingReplyUseCase: SavePendingReplyUseCase
) {

    @PostMapping("/create-pending")
    suspend fun createPendingReply() {
        val session = Session.getDefaultInstance(Properties())
        val mimeMessage = MimeMessage(session)
        val emailMessage = EmailMessage(
            id = "test-${System.currentTimeMillis()}",
            from = "sender@example.com",
            to = listOf("recipient@example.com"),
            subject = "Test Email Subject " + System.currentTimeMillis(),
            content = "This is a test email content.",
            receivedDate = Date(),
            rawMessage = mimeMessage
        )

        savePendingReplyUseCase.execute(
            SavePendingReplyUseCaseIn(
                originalMessage = emailMessage,
                replyContent = "This is a generated reply for testing.",
                providerType = EmailProperties.EmailProviderType.GMAIL,
                toEmail = "recipient@example.com"
            )
        )
    }
}
