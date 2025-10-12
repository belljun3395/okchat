package com.okestro.okchat.permission.application.dto

import com.okestro.okchat.permission.model.DocumentPathPermission

data class GrantPathPermissionUseCaseIn(
    val userId: Long,
    val documentPath: String,
    val spaceKey: String? = null,
    val grantedBy: Long? = null
)

data class GrantPathPermissionUseCaseOut(
    val permission: DocumentPathPermission
)
