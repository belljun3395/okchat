package com.okestro.okchat.permission.application.dto

data class RevokeAllUserPermissionsUseCaseIn(
    val userId: Long
)

data class RevokeAllUserPermissionsUseCaseOut(
    val success: Boolean,
    val userId: Long
)
