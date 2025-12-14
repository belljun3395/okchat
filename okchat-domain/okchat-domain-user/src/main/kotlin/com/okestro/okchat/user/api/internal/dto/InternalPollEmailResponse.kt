package com.okestro.okchat.user.api.internal.dto

data class InternalPollEmailResponse(
    val messagesCount: Int,
    val eventsCount: Int,
    val status: String = "SUCCESS",
    val message: String? = null
)
