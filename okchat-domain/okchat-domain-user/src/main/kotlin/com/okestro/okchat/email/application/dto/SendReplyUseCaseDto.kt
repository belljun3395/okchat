package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.PendingEmailReply

data class SendReplyUseCaseIn(
    val id: Long
)

data class SendReplyUseCaseOut(
    val result: Result<PendingEmailReply>
)
