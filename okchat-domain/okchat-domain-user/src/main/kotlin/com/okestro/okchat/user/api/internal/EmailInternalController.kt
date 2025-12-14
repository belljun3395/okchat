package com.okestro.okchat.user.api.internal

import com.okestro.okchat.email.application.usecase.PollEmailUseCase
import com.okestro.okchat.user.api.internal.dto.InternalPollEmailResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/api/v1/emails")
class EmailInternalController(
    private val pollEmailUseCase: PollEmailUseCase
) {

    @PostMapping("/poll/{knowledgeBaseId}")
    suspend fun pollEmails(@PathVariable knowledgeBaseId: Long): InternalPollEmailResponse {
        val result = pollEmailUseCase.execute(knowledgeBaseId)
        return InternalPollEmailResponse(
            messagesCount = result.messagesCount,
            eventsCount = result.eventsCount
        )
    }
}
