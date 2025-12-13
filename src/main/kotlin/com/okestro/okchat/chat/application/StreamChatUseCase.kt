package com.okestro.okchat.chat.application

import com.okestro.okchat.chat.application.dto.StreamChatUseCaseIn
import com.okestro.okchat.chat.service.DocumentBaseChatService
import com.okestro.okchat.chat.service.dto.ChatServiceRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class StreamChatUseCase(
    private val documentBaseChatService: DocumentBaseChatService
) {
    suspend fun execute(input: StreamChatUseCaseIn): Flux<String> {
        return documentBaseChatService.chat(
            ChatServiceRequest(
                message = input.message,
                isDeepThink = input.isDeepThink,
                keywords = input.keywords,
                sessionId = input.sessionId,
                userEmail = input.userEmail
            )
        )
    }
}
