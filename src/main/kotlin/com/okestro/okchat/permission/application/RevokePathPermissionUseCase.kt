package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.RevokePathPermissionUseCaseIn
import com.okestro.okchat.permission.application.dto.RevokePathPermissionUseCaseOut
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class RevokePathPermissionUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    suspend fun execute(useCaseIn: RevokePathPermissionUseCaseIn): RevokePathPermissionUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val (userId, documentPaths) = useCaseIn

            if (documentPaths.isEmpty()) {
                return@withContext RevokePathPermissionUseCaseOut(success = true, revokedCount = 0)
            }

            documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, documentPaths)
            log.info { "Bulk path permissions revoked: user_id=$userId, count=${documentPaths.size}" }

            RevokePathPermissionUseCaseOut(success = true, revokedCount = documentPaths.size)
        }
}
