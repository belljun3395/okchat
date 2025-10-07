package com.okestro.okchat.permission.service

import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.user.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for filtering search results based on user permissions
 *
 * Responsibilities:
 * - Filter search results by user email
 * - Handle both document-specific and path-based permissions
 * - Separate from PermissionService to follow Single Responsibility Principle
 */
@Service
class DocumentPermissionFilterService(
    private val permissionService: PermissionService,
    private val userService: UserService
) {

    /**
     * Filter search results for a user identified by email
     *
     * @param results Original search results
     * @param userEmail User email to identify and check permissions
     * @return Filtered list containing only accessible documents
     */
    fun filterByUserEmail(results: List<SearchResult>, userEmail: String): List<SearchResult> {
        if (results.isEmpty()) {
            log.debug { "[PermissionFilter] No results to filter" }
            return emptyList()
        }

        // 1. Identify user
        val user = userService.findByEmail(userEmail)
        if (user == null) {
            log.warn { "[PermissionFilter] User not found or inactive: email=$userEmail, filtering all results" }
            return emptyList()
        }

        log.debug { "[PermissionFilter] Filtering ${results.size} results for user: id=${user.id}, email=$userEmail" }

        // 2. Filter by permissions
        val filtered = permissionService.filterSearchResults(results, user.id!!)

        log.info {
            "[PermissionFilter] Filtered results for $userEmail: " +
                "${results.size} -> ${filtered.size} (removed ${results.size - filtered.size})"
        }

        return filtered
    }

    /**
     * Check if user has permission to access a document
     *
     * @param userEmail User email
     * @param documentId Document ID to check
     * @return true if user has access, false otherwise
     */
    fun hasAccess(userEmail: String, documentId: String): Boolean {
        val user = userService.findByEmail(userEmail) ?: return false
        return permissionService.hasPermission(user.id!!, documentId)
    }
}
