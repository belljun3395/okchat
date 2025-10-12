package com.okestro.okchat.permission.application.dto

import com.okestro.okchat.permission.model.DocumentPathPermission

data class GrantDenyPathPermissionUseCaseIn(
    val userId: Long,
    val documentPath: String,
    val spaceKey: String? = null,
    val grantedBy: Long? = null
)

data class GrantDenyPathPermissionUseCaseOut(
    val permission: DocumentPathPermission
)
