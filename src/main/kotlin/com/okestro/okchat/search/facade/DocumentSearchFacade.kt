package com.okestro.okchat.search.facade

import com.okestro.okchat.permission.service.PermissionService
import com.okestro.okchat.search.model.ContentSearchResults
import com.okestro.okchat.search.model.KeywordSearchResults
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.PathSearchResults
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.model.TitleSearchResults
import com.okestro.okchat.search.service.DocumentSearchService
import com.okestro.okchat.user.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Facade for document search with permission filtering
 *
 * Design:
 * - Wraps DocumentSearchService to add permission layer
 * - User identification happens before calling this facade
 * - Delegates actual search to DocumentSearchService (unchanged)
 * - Applies permission filtering using PermissionService
 *
 * Flow:
 * 1. Identify user (by email)
 * 2. Execute search (DocumentSearchService)
 * 3. Filter results (PermissionService)
 * 4. Return filtered results
 */
@Service
class DocumentSearchFacade(
    private val documentSearchService: DocumentSearchService,
    private val permissionService: PermissionService,
    private val userService: UserService
) {

    /**
     * Search documents with permission filtering based on user email
     *
     * @param userEmail Email address to identify the user
     * @param titles Title search terms
     * @param contents Content search terms
     * @param paths Path search terms
     * @param keywords Keyword search terms
     * @param topK Maximum results per search type
     * @return Filtered multi-search results (only accessible documents)
     */
    suspend fun searchWithPermissions(
        userEmail: String,
        titles: SearchTitles? = null,
        contents: SearchContents? = null,
        paths: SearchPaths? = null,
        keywords: SearchKeywords? = null,
        topK: Int = 50
    ): MultiSearchResult {
        log.info { "[DocumentSearchFacade] Search request from user: email=$userEmail" }

        // 1. Identify user
        val user = userService.findByEmail(userEmail)
        if (user == null) {
            log.warn { "[DocumentSearchFacade] User not found or inactive: email=$userEmail" }
            return MultiSearchResult.empty()
        }

        log.debug { "[DocumentSearchFacade] User identified: id=${user.id}, email=${user.email}" }

        // 2. Execute search (no permission checks yet)
        val searchResult = documentSearchService.multiSearch(
            titles = titles,
            contents = contents,
            paths = paths,
            keywords = keywords,
            topK = topK
        )

        // 3. Filter each result set by permissions
        val userId = user.id!!
        val filteredKeywordResults = permissionService.filterSearchResults(searchResult.keywordResults.results, userId)
        val filteredTitleResults = permissionService.filterSearchResults(searchResult.titleResults.results, userId)
        val filteredContentResults = permissionService.filterSearchResults(searchResult.contentResults.results, userId)
        val filteredPathResults = permissionService.filterSearchResults(searchResult.pathResults.results, userId)

        val filteredResult = MultiSearchResult.fromMap(
            mapOf(
                SearchType.KEYWORD to KeywordSearchResults(filteredKeywordResults),
                SearchType.TITLE to TitleSearchResults(filteredTitleResults),
                SearchType.CONTENT to ContentSearchResults(filteredContentResults),
                SearchType.PATH to PathSearchResults(filteredPathResults)
            )
        )

        // 4. Log statistics
        val originalTotal = searchResult.keywordResults.results.size +
            searchResult.titleResults.results.size +
            searchResult.contentResults.results.size +
            searchResult.pathResults.results.size
        val filteredTotal = filteredKeywordResults.size +
            filteredTitleResults.size +
            filteredContentResults.size +
            filteredPathResults.size
        log.info {
            "[DocumentSearchFacade] Search completed: " +
                "user_id=${user.id}, " +
                "original_results=$originalTotal, " +
                "filtered_results=$filteredTotal, " +
                "filtered_out=${originalTotal - filteredTotal}"
        }

        return filteredResult
    }

    /**
     * Check if user has permission to access a specific document
     *
     * @param userEmail Email address to identify the user
     * @param documentId Document ID to check
     * @return true if user has access, false otherwise
     */
    fun hasDocumentAccess(userEmail: String, documentId: String): Boolean {
        val user = userService.findByEmail(userEmail) ?: return false
        return permissionService.hasPermission(user.id!!, documentId)
    }
}
