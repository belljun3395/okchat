package com.okestro.okchat.permission.application.dto

import com.okestro.okchat.permission.model.entity.DocumentPathPermission

data class GetPathPermissionsUseCaseIn(
    val documentPath: String
)

data class GetPathPermissionsUseCaseOut(
    val permissions: List<DocumentPathPermission>
)
