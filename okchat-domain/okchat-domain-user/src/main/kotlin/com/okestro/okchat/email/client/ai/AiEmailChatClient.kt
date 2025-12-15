package com.okestro.okchat.email.client.ai

interface AiEmailChatClient {
    suspend fun processEmailQuestion(subject: String, content: String): String
}
