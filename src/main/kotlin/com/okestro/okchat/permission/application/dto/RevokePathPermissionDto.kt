package com.okestro.okchat.permission.application.dto

data class RevokePathPermissionUseCaseIn(
    val userId: Long,
    val documentPaths: List<String>
)

data class RevokePathPermissionUseCaseOut(
    val success: Boolean,
    val revokedCount: Int
)
