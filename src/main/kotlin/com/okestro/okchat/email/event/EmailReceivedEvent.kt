package com.okestro.okchat.email.event

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.provider.EmailMessage

/**
 * Event data class for received emails
 * Pure data class without extending ApplicationEvent for reactive usage
 */
data class EmailReceivedEvent(
    val message: EmailMessage,
    val providerType: EmailProperties.EmailProviderType,
    val knowledgeBaseId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
