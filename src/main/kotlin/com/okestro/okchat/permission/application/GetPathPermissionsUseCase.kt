package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GetPathPermissionsUseCaseIn
import com.okestro.okchat.permission.application.dto.GetPathPermissionsUseCaseOut
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import org.springframework.stereotype.Service

@Service
class GetPathPermissionsUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    fun execute(useCaseIn: GetPathPermissionsUseCaseIn): GetPathPermissionsUseCaseOut {
        val permissions = documentPathPermissionRepository.findByDocumentPath(useCaseIn.documentPath)
        return GetPathPermissionsUseCaseOut(permissions)
    }
}
