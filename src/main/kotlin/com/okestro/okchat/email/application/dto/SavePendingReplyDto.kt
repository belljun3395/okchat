package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.provider.EmailMessage

data class SavePendingReplyUseCaseIn(
    val originalMessage: EmailMessage,
    val replyContent: String,
    val providerType: EmailProperties.EmailProviderType,
    val toEmail: String,
    val knowledgeBaseId: Long
)

data class SavePendingReplyUseCaseOut(
    val pendingReply: PendingEmailReply
)
