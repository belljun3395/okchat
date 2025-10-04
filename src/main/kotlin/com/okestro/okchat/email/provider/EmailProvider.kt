package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailProperties
import jakarta.mail.Message

interface EmailProvider {
    suspend fun connect(): Boolean

    suspend fun disconnect()

    suspend fun fetchNewMessages(): List<EmailMessage>

    suspend fun isConnected(): Boolean

    fun getProviderType(): EmailProperties.EmailProviderType
}

data class EmailMessage(
    val id: String,
    val from: String,
    val to: List<String>,
    val subject: String,
    val content: String,
    val receivedDate: java.util.Date,
    val isRead: Boolean = false,
    val rawMessage: Message
)
