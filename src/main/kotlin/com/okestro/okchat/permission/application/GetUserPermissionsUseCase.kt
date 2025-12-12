package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GetUserPermissionsUseCaseIn
import com.okestro.okchat.permission.application.dto.GetUserPermissionsUseCaseOut
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetUserPermissionsUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    suspend fun execute(useCaseIn: GetUserPermissionsUseCaseIn): GetUserPermissionsUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val permissions = documentPathPermissionRepository.findByUserId(useCaseIn.userId)
            GetUserPermissionsUseCaseOut(permissions)
        }
}
