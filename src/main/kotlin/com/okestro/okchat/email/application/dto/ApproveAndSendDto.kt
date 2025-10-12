package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.PendingEmailReply

data class ApproveAndSendUseCaseIn(
    val id: Long,
    val reviewedBy: String
)

data class ApproveAndSendUseCaseOut(
    val result: Result<PendingEmailReply>
)
