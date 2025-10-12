package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.ReviewStatus

data class CountByStatusUseCaseIn(
    val status: ReviewStatus
)

data class CountByStatusUseCaseOut(
    val count: Long
)
