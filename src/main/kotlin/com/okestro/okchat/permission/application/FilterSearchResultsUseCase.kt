package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseIn
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseOut
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class FilterSearchResultsUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {

    fun execute(useCaseIn: FilterSearchResultsUseCaseIn): FilterSearchResultsUseCaseOut {
        val (results, userId) = useCaseIn
        if (results.isEmpty()) {
            return FilterSearchResultsUseCaseOut(emptyList())
        }

        log.debug { "[PermissionFilter] Filtering ${results.size} results for user_id=$userId" }

        val allPermissions = documentPathPermissionRepository.findByUserId(userId)

        log.debug {
            "[PermissionFilter] User has ${allPermissions.size} total path permissions " +
                "(READ: ${allPermissions.count { it.permissionLevel == PermissionLevel.READ }}, " +
                "DENY: ${allPermissions.count { it.permissionLevel == PermissionLevel.DENY }})"
        }

        val filtered = results.filter { result ->
            val docPath = result.path

            val matchingPermissions = allPermissions.filter { perm ->
                isPathMatchingOrParent(docPath, perm.documentPath)
            }

            if (matchingPermissions.isEmpty()) {
                log.trace { "[PermissionFilter] No permissions match: path=$docPath" }
                return@filter false
            }

            val mostSpecific = matchingPermissions.maxByOrNull { it.documentPath.length }!!

            val isAllowed = mostSpecific.permissionLevel == PermissionLevel.READ

            if (!isAllowed) {
                log.trace {
                    "[PermissionFilter] Denied by most specific permission: " +
                        "path=$docPath, matched=${mostSpecific.documentPath}, level=${mostSpecific.permissionLevel}"
                }
            }

            isAllowed
        }

        val filteredCount = results.size - filtered.size
        if (filteredCount > 0) {
            log.info { "[PermissionFilter] Filtered out $filteredCount documents (${results.size} -> ${filtered.size})" }
        }

        return FilterSearchResultsUseCaseOut(filtered)
    }

    private fun isPathMatchingOrParent(documentPath: String, grantedPath: String): Boolean {
        if (documentPath.isEmpty() || grantedPath.isEmpty()) return false
        if (documentPath == grantedPath) return true
        return documentPath.startsWith("$grantedPath >")
    }
}
