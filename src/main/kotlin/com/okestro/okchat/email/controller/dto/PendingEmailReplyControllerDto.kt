package com.okestro.okchat.email.controller.dto

/**
 * Request body for reviewing emails
 */
data class ReviewRequest(
    val reviewedBy: String,
    val rejectionReason: String? = null
)

/**
 * Generic API response
 */
data class EmailApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any?
)
