package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.PendingEmailReply

data class ApproveReplyUseCaseIn(
    val id: Long,
    val reviewedBy: String
)

data class ApproveReplyUseCaseOut(
    val result: Result<PendingEmailReply>
)
