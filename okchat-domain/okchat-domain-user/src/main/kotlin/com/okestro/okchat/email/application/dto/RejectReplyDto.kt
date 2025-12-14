package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.PendingEmailReply

data class RejectReplyUseCaseIn(
    val id: Long,
    val reviewedBy: String,
    val rejectionReason: String? = null
)

data class RejectReplyUseCaseOut(
    val result: Result<PendingEmailReply>
)
