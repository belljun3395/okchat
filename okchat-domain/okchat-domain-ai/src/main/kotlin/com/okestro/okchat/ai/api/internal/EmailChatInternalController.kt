package com.okestro.okchat.ai.api.internal

import com.okestro.okchat.email.service.EmailChatService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/api/v1/ai/email-chat")
class EmailChatInternalController(
    private val emailChatService: EmailChatService
) {

    @PostMapping
    suspend fun processEmailQuestion(
        @RequestBody request: InternalEmailChatRequest
    ): InternalEmailChatResponse {
        val chunks = emailChatService.processEmailQuestion(
            emailSubject = request.subject,
            emailContent = request.content
        ).collectList().awaitSingle()

        return InternalEmailChatResponse(
            answer = chunks.joinToString(separator = "")
        )
    }

    data class InternalEmailChatRequest(
        val subject: String,
        val content: String
    )

    data class InternalEmailChatResponse(
        val answer: String
    )
}
