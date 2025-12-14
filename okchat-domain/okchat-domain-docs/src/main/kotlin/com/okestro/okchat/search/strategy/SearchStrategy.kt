package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.model.SearchCriteria
import com.okestro.okchat.search.model.SearchResult

/**
 * Strategy Pattern for document search.
 *
 * Responsibility:
 * - Define the contract for single-type search operations
 * - Accept SearchCriteria for consistency with MultiSearchStrategy
 *
 * Benefits:
 * - Consistent with MultiSearchStrategy (both use SearchCriteria)
 * - Extensible: New search types supported without interface changes
 * - Type-safe: SearchCriteria carries both type and query information
 */
interface SearchStrategy {
    /**
     * Execute search with given criteria.
     *
     * @param criteria Search criteria containing type and query information
     * @param topK Maximum number of results to return
     * @return List of search results
     */
    suspend fun search(criteria: SearchCriteria, topK: Int): List<SearchResult>

    /**
     * Get the name of this search strategy (for logging)
     */
    fun getName(): String
}
