package com.okestro.okchat.batch.client.user.dto

data class InternalPollEmailResponse(
    val messagesCount: Int,
    val eventsCount: Int,
    val status: String = "SUCCESS",
    val message: String? = null
)
