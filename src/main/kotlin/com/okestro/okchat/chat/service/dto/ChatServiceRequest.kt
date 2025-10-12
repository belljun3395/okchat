package com.okestro.okchat.chat.service.dto

data class ChatServiceRequest(
    val message: String,
    val isDeepThink: Boolean = false,
    val keywords: List<String> = emptyList(),
    val sessionId: String? = null,
    val userEmail: String? = null
)
