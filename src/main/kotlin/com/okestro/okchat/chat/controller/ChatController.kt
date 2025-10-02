
package com.okestro.okchat.chat.controller

import com.okestro.okchat.chat.service.ChatService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun chat(@RequestBody chatRequest: ChatRequest): Flux<String> {
        return chatService.chat(chatRequest.message, chatRequest.keywords)
    }
}

data class ChatRequest(
    val message: String,
    val keywords: List<String>? = null
)
