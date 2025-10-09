
package com.okestro.okchat.chat.controller

import com.okestro.okchat.chat.service.ChatServiceRequest
import com.okestro.okchat.chat.service.DocumentBaseChatService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.reactive.asFlow
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/chat")
@Validated
class ChatController(
    private val documentBaseChatService: DocumentBaseChatService
) {

    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun chat(@Valid @RequestBody chatRequest: ChatRequest): Flow<String> {
        val startTime = System.currentTimeMillis()
        
        log.info { "Chat request received: sessionId=${chatRequest.sessionId}, messageLength=${chatRequest.message.length}" }
        
        return documentBaseChatService.chat(
            ChatServiceRequest(
                message = chatRequest.message,
                isDeepThink = chatRequest.isDeepThink,
                keywords = chatRequest.keywords ?: emptyList(),
                sessionId = chatRequest.sessionId
            )
        ).asFlow()
            .onStart {
                log.debug { "Starting chat stream for session: ${chatRequest.sessionId}" }
            }
            .onCompletion {
                val duration = System.currentTimeMillis() - startTime
                log.info { "Chat stream completed: sessionId=${chatRequest.sessionId}, duration=${duration}ms" }
            }
    }
}

data class ChatRequest(
    @field:NotBlank(message = "Message cannot be blank")
    @field:Size(max = 4000, message = "Message cannot exceed 4000 characters")
    val message: String,
    
    @field:Size(max = 10, message = "Keywords list cannot exceed 10 items")
    val keywords: List<String>? = null,
    
    @field:Size(max = 100, message = "Session ID cannot exceed 100 characters")
    val sessionId: String? = null,
    
    val isDeepThink: Boolean = false
)
