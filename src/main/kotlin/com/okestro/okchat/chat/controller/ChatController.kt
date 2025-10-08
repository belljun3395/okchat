
package com.okestro.okchat.chat.controller

import com.okestro.okchat.chat.service.ChatServiceRequest
import com.okestro.okchat.chat.service.DocumentBaseChatService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val documentBaseChatService: DocumentBaseChatService
) {

    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun chat(@RequestBody chatRequest: ChatRequest): Flux<String> {
        return documentBaseChatService.chat(
            ChatServiceRequest(
                message = chatRequest.message,
                isDeepThink = chatRequest.isDeepThink,
                keywords = chatRequest.keywords ?: emptyList(),
                sessionId = chatRequest.sessionId
            )
        )
    }
}

data class ChatRequest(
    val message: String,
    val keywords: List<String>? = null,
    val sessionId: String? = null,
    val isDeepThink: Boolean = false
)
