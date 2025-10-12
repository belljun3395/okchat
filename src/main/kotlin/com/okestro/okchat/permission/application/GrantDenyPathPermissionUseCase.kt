package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GrantDenyPathPermissionUseCaseIn
import com.okestro.okchat.permission.application.dto.GrantDenyPathPermissionUseCaseOut
import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class GrantDenyPathPermissionUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: GrantDenyPathPermissionUseCaseIn): GrantDenyPathPermissionUseCaseOut =
        withContext(Dispatchers.IO) {
            val (userId, documentPath, spaceKey, grantedBy) = useCaseIn

            val existing = documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, documentPath)
            if (existing != null && existing.permissionLevel == PermissionLevel.DENY) {
                log.debug { "Path DENY permission already exists: user_id=$userId, path=$documentPath" }
                return@withContext GrantDenyPathPermissionUseCaseOut(existing)
            }

            if (existing != null && existing.permissionLevel == PermissionLevel.READ) {
                documentPathPermissionRepository.delete(existing)
                log.info { "Replaced READ path permission with DENY: user_id=$userId, path=$documentPath" }
            }

            val permission = DocumentPathPermission(
                userId = userId,
                documentPath = documentPath,
                spaceKey = spaceKey,
                permissionLevel = PermissionLevel.DENY,
                grantedBy = grantedBy
            )

            val savedPermission = documentPathPermissionRepository.save(permission)
            log.info { "Path DENY permission granted: user_id=$userId, path=$documentPath, space_key=$spaceKey" }

            GrantDenyPathPermissionUseCaseOut(savedPermission)
        }
}
