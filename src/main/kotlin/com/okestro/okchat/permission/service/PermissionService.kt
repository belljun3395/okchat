package com.okestro.okchat.permission.service

import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import com.okestro.okchat.search.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

// TODO: refactor this class to can test it more easily
/**
 * Service for document permission management and filtering (Path-based only)
 */
@Service
class PermissionService(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {

    /**
     * Filter search results to only include documents the user has permission to access
     *
     * Uses path-based permissions with "Most Specific Wins" principle:
     * - Find all matching permissions (READ and DENY) for each document path
     * - Use the MOST SPECIFIC (longest matching) path's permission
     * - This allows exceptions: DENY parent, but READ specific child
     *
     * Example:
     * - "업무일지" DENY
     * - "업무일지 > 김종준" READ
     * - Result: "업무일지 > 김종준" is allowed (more specific READ wins)
     * - Result: "업무일지 > 다른사람" is denied (only parent DENY matches)
     *
     * @param results Original search results
     * @param userId User ID to check permissions for
     * @return Filtered list containing only accessible documents
     */
    fun filterSearchResults(results: List<SearchResult>, userId: Long): List<SearchResult> {
        if (results.isEmpty()) {
            return emptyList()
        }

        log.debug { "[PermissionFilter] Filtering ${results.size} results for user_id=$userId" }

        // Get all path permissions (both READ and DENY)
        val allPermissions = documentPathPermissionRepository.findByUserId(userId)

        log.debug {
            "[PermissionFilter] User has ${allPermissions.size} total path permissions " +
                "(READ: ${allPermissions.count { it.permissionLevel == PermissionLevel.READ }}, " +
                "DENY: ${allPermissions.count { it.permissionLevel == PermissionLevel.DENY }})"
        }

        // Filter results using "Most Specific Wins" principle
        val filtered = results.filter { result ->
            val docPath = result.path

            // Find all permissions that match this document path
            val matchingPermissions = allPermissions.filter { perm ->
                isPathMatchingOrParent(docPath, perm.documentPath)
            }

            if (matchingPermissions.isEmpty()) {
                log.trace { "[PermissionFilter] No permissions match: path=$docPath" }
                return@filter false
            }

            // Find the most specific (longest) matching permission
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

        return filtered
    }

    /**
     * Check if document path matches granted path (hierarchical)
     * Supports both exact match and parent path matching
     *
     * @param documentPath Full document path
     * @param grantedPath Granted/denied path (can be parent path)
     * @return true if documentPath is same as or child of grantedPath
     *
     * Examples:
     * - isPathMatching("A > B > C", "A > B") = true (child)
     * - isPathMatching("A > B", "A > B") = true (exact)
     * - isPathMatching("A > B", "A > B > C") = false (parent)
     */
    private fun isPathMatchingOrParent(documentPath: String, grantedPath: String): Boolean {
        if (documentPath.isEmpty() || grantedPath.isEmpty()) return false

        // Exact match
        if (documentPath == grantedPath) return true

        // Hierarchical match: document path starts with granted path
        // Example: "팀회의 > 2025 > 1월 > 회의록" starts with "팀회의 > 2025"
        return documentPath.startsWith("$grantedPath >")
    }

    /**
     * Grant READ permission to a user for all documents under a path
     * Automatically cleans up redundant child path READ permissions
     *
     * If granting READ to "A > B", removes READ from:
     * - "A > B > C"
     * - "A > B > C > D"
     * (keeps DENY permissions)
     */
    @Transactional("transactionManager")
    fun grantPathPermission(
        userId: Long,
        documentPath: String,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): DocumentPathPermission {
        // Check if permission already exists
        val existing = documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, documentPath)
        if (existing != null) {
            log.debug { "Path permission already exists: user_id=$userId, path=$documentPath" }
            return existing
        }

        // Clean up redundant child path READ permissions
        // When granting "A > B" READ, remove "A > B > C", "A > B > C > D" READ permissions
        val userPathPermissions = documentPathPermissionRepository.findByUserId(userId)
        val redundantChildPaths = userPathPermissions.filter { pathPerm ->
            // Only clean up READ permissions that are children of this path
            pathPerm.permissionLevel == PermissionLevel.READ &&
                pathPerm.documentPath != documentPath &&
                isPathMatchingOrParent(pathPerm.documentPath, documentPath)
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

        return documentPathPermissionRepository.save(permission).also {
            log.info { "Path permission granted: user_id=$userId, path=$documentPath, space_key=$spaceKey" }
        }
    }

    /**
     * Grant DENY permission to a user for a specific path
     *
     * With "Most Specific Wins" principle:
     * - DENY blocks access to the path and its children
     * - BUT more specific child READ permissions override this DENY
     * - Only removes LESS specific (parent) READ permissions
     *
     * Example:
     * - "A > B" READ exists
     * - Grant DENY "A > B > C"
     * - Result: "A > B" READ remains (parent of DENY)
     * - Result: "A > B > C" is denied
     * - Result: "A > B > C > D" is denied (child of DENY, no more specific permission)
     */
    @Transactional("transactionManager")
    fun grantDenyPathPermission(
        userId: Long,
        documentPath: String,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): DocumentPathPermission {
        // Check if DENY permission already exists
        val existing = documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, documentPath)
        if (existing != null && existing.permissionLevel == PermissionLevel.DENY) {
            log.debug { "Path DENY permission already exists: user_id=$userId, path=$documentPath" }
            return existing
        }

        // If READ permission exists for same path, delete it first (DENY replaces READ at same level)
        if (existing != null && existing.permissionLevel == PermissionLevel.READ) {
            documentPathPermissionRepository.delete(existing)
            log.info { "Replaced READ path permission with DENY: user_id=$userId, path=$documentPath" }
        }

        // DON'T remove child READ permissions - they act as exceptions to this DENY
        // This allows: DENY "업무일지" but READ "업무일지 > 김종준"

        val permission = DocumentPathPermission(
            userId = userId,
            documentPath = documentPath,
            spaceKey = spaceKey,
            permissionLevel = PermissionLevel.DENY,
            grantedBy = grantedBy
        )

        return documentPathPermissionRepository.save(permission).also {
            log.info { "Path DENY permission granted: user_id=$userId, path=$documentPath, space_key=$spaceKey" }
        }
    }

    /**
     * Revoke multiple path-based permissions
     */
    @Transactional("transactionManager")
    fun revokeBulkPathPermissions(userId: Long, documentPaths: List<String>) {
        documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, documentPaths)
        log.info { "Bulk path permissions revoked: user_id=$userId, count=${documentPaths.size}" }
    }

    /**
     * Grant permissions to multiple paths at once
     */
    @Transactional("transactionManager")
    fun grantBulkPathPermissions(
        userId: Long,
        documentPaths: List<String>,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): Int {
        var grantedCount = 0

        documentPaths.forEach { path ->
            try {
                grantPathPermission(userId, path, spaceKey, grantedBy)
                grantedCount++
            } catch (e: Exception) {
                log.warn { "Failed to grant path permission: user_id=$userId, path=$path, error=${e.message}" }
            }
        }

        log.info { "Bulk path permissions granted: user_id=$userId, granted=$grantedCount/${documentPaths.size}" }
        return grantedCount
    }

    /**
     * Grant DENY permissions to multiple paths at once
     */
    @Transactional("transactionManager")
    fun grantBulkDenyPathPermissions(
        userId: Long,
        documentPaths: List<String>,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): Int {
        var grantedCount = 0

        documentPaths.forEach { path ->
            try {
                grantDenyPathPermission(userId, path, spaceKey, grantedBy)
                grantedCount++
            } catch (e: Exception) {
                log.warn { "Failed to grant DENY path permission: user_id=$userId, path=$path, error=${e.message}" }
            }
        }

        log.info { "Bulk DENY path permissions granted: user_id=$userId, granted=$grantedCount/${documentPaths.size}" }
        return grantedCount
    }

    /**
     * Revoke all permissions for a user
     */
    @Transactional("transactionManager")
    fun revokeAllPermissionsForUser(userId: Long) {
        documentPathPermissionRepository.deleteByUserId(userId)
        log.info { "All path permissions revoked for user: user_id=$userId" }
    }

    /**
     * Get all path permissions for a user
     */
    fun getUserPathPermissions(userId: Long): List<DocumentPathPermission> {
        return documentPathPermissionRepository.findByUserId(userId)
    }

    /**
     * Get all permissions for a path
     */
    fun getPathPermissions(documentPath: String): List<DocumentPathPermission> {
        return documentPathPermissionRepository.findByDocumentPath(documentPath)
    }
}
