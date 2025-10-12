package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.ReviewStatus

data class CountByStatusUseCaseIn(
    val status: ReviewStatus
)

data class CountByStatusUseCaseOut(
    val count: Long
)
