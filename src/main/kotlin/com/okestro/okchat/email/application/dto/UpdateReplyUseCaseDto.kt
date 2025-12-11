package com.okestro.okchat.email.application.dto

data class UpdateReplyUseCaseIn(
    val id: Long,
    val replyContent: String
)

data class UpdateReplyUseCaseOut(
    val success: Boolean
)
