package com.okestro.okchat.email.client.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class WebClientAiEmailChatClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${internal.services.ai.url:http://localhost:8080}")
    private val aiServiceBaseUrl: String
) : AiEmailChatClient {

    private val client: WebClient = webClientBuilder
        .baseUrl(aiServiceBaseUrl)
        .build()

    override suspend fun processEmailQuestion(subject: String, content: String): String {
        val response = client.post()
            .uri("/internal/api/v1/ai/email-chat")
            .bodyValue(InternalEmailChatRequest(subject = subject, content = content))
            .retrieve()
            .awaitBody<InternalEmailChatResponse>()

        return response.answer
    }

    private data class InternalEmailChatRequest(
        val subject: String,
        val content: String
    )

    private data class InternalEmailChatResponse(
        val answer: String
    )
}
