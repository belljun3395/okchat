package com.okestro.okchat.confluence.api.internal.dto

data class InternalConfluenceSyncResponse(
    val status: String,
    val message: String? = null
)
