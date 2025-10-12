package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GetUserPermissionsUseCaseIn
import com.okestro.okchat.permission.application.dto.GetUserPermissionsUseCaseOut
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import org.springframework.stereotype.Service

@Service
class GetUserPermissionsUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    fun execute(useCaseIn: GetUserPermissionsUseCaseIn): GetUserPermissionsUseCaseOut {
        val permissions = documentPathPermissionRepository.findByUserId(useCaseIn.userId)
        return GetUserPermissionsUseCaseOut(permissions)
    }
}
