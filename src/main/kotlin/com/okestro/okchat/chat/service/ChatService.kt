package com.okestro.okchat.chat.service

import reactor.core.publisher.Flux

interface ChatService {
    suspend fun chat(
        chatServiceRequest: ChatServiceRequest
    ): Flux<String>
}

data class ChatServiceRequest(
    val message: String,
    val isDeepThink: Boolean = false,
    val keywords: List<String> = emptyList(),
    val sessionId: String? = null
)
