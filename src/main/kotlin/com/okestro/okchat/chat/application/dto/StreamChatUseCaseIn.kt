package com.okestro.okchat.chat.application.dto

data class StreamChatUseCaseIn(
    val message: String,
    val isDeepThink: Boolean = false,
    val keywords: List<String> = emptyList(),
    val sessionId: String? = null,
    val userEmail: String? = null
)
