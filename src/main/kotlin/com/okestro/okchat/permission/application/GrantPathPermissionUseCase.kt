package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GrantPathPermissionUseCaseIn
import com.okestro.okchat.permission.application.dto.GrantPathPermissionUseCaseOut
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.model.entity.DocumentPathPermission
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class GrantPathPermissionUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    suspend fun execute(useCaseIn: GrantPathPermissionUseCaseIn): GrantPathPermissionUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val (userId, documentPath, spaceKey, grantedBy) = useCaseIn

            val existing = documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, documentPath)
            if (existing != null) {
                log.debug { "Path permission already exists: user_id=$userId, path=$documentPath" }
                return@withContext GrantPathPermissionUseCaseOut(existing)
            }

            val userPathPermissions = documentPathPermissionRepository.findByUserId(userId)
            val redundantChildPaths = userPathPermissions.filter {
                it.permissionLevel == PermissionLevel.READ &&
                    it.documentPath != documentPath &&
                    isPathMatchingOrParent(it.documentPath, documentPath)
            }

            if (redundantChildPaths.isNotEmpty()) {
                val pathsToDelete = redundantChildPaths.map { it.documentPath }
                documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, pathsToDelete)
                log.info {
                    "Cleaned up ${redundantChildPaths.size} redundant child path READ permissions: " +
                        "user_id=$userId, parent_path=$documentPath, removed_paths=$pathsToDelete"
                }
            }

            val permission = DocumentPathPermission(
                userId = userId,
                documentPath = documentPath,
                spaceKey = spaceKey,
                permissionLevel = PermissionLevel.READ,
                grantedBy = grantedBy
            )

            val savedPermission = documentPathPermissionRepository.save(permission)
            log.info { "Path permission granted: user_id=$userId, path=$documentPath, space_key=$spaceKey" }
            GrantPathPermissionUseCaseOut(savedPermission)
        }

    private fun isPathMatchingOrParent(documentPath: String, grantedPath: String): Boolean {
        if (documentPath.isEmpty() || grantedPath.isEmpty()) return false
        if (documentPath == grantedPath) return true
        return documentPath.startsWith("$grantedPath >")
    }
}
