package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.service.dto.ChatServiceRequest
import reactor.core.publisher.Flux

interface ChatService {
    suspend fun chat(
        chatServiceRequest: ChatServiceRequest
    ): Flux<String>
}
