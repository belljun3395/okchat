package com.okestro.okchat.permission.service

import com.okestro.okchat.search.model.Document
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.service.DocumentSearchService
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for filtering search results based on user permissions
 */
@Service
class DocumentPermissionService(
    private val permissionService: PermissionService,
    private val findUserByEmailUseCase: FindUserByEmailUseCase,
    private val documentSearchService: DocumentSearchService
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
        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(userEmail)).user
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

    fun searchAllPaths(): List<String> {
        return documentSearchService.searchAllPaths()
    }

    suspend fun searchAllByPath(documentPath: String): List<Document> {
        return documentSearchService.searchAllByPath(documentPath)
    }
}
