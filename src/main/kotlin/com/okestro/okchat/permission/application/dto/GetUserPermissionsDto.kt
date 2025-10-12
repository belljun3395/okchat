package com.okestro.okchat.permission.application.dto

import com.okestro.okchat.permission.model.DocumentPathPermission

data class GetUserPermissionsUseCaseIn(
    val userId: Long
)

data class GetUserPermissionsUseCaseOut(
    val permissions: List<DocumentPathPermission>
)
