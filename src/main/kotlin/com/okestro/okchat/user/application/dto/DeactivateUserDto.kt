package com.okestro.okchat.user.application.dto

data class DeactivateUserUseCaseIn(
    val userId: Long
)

data class DeactivateUserUseCaseOut(
    val success: Boolean,
    val userId: Long
)
