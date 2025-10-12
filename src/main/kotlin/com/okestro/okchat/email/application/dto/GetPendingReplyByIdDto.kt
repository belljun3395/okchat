package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.PendingEmailReply

data class GetPendingReplyByIdUseCaseIn(
    val id: Long
)

data class GetPendingReplyByIdUseCaseOut(
    val reply: PendingEmailReply?
)
