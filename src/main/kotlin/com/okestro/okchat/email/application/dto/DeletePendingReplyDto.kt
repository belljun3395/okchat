package com.okestro.okchat.email.application.dto

data class DeletePendingReplyUseCaseIn(
    val id: Long
)

data class DeletePendingReplyUseCaseOut(
    val success: Boolean
)
