package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchCriteria

/**
 * Strategy interface for multi-search operations.
 *
 * Responsibility: Define the contract for executing multi-type searches
 *
 * Design: Uses List<SearchCriteria> for Open-Closed Principle
 * - Open for extension: New search types can be added without interface changes
 * - Closed for modification: Interface remains stable
 *
 * Implementations can provide different search strategies:
 * - HybridMultiSearchStrategy: Batched hybrid search (current implementation)
 * - SequentialMultiSearchStrategy: Execute searches sequentially
 * - ParallelMultiSearchStrategy: Execute searches in parallel
 *
 * Benefits of Strategy pattern with polymorphic criteria:
 * - Flexible: Easy to switch search strategies
 * - Extensible: Add new search types without changing interface
 * - Testable: Each strategy can be tested independently
 * - Clear: Each strategy has a single, well-defined responsibility
 */
interface MultiSearchStrategy {

    /**
     * Execute multi-search across different search types.
     *
     * @param searchCriteria List of search criteria (keywords, titles, contents, paths, etc.)
     *                       Each criteria knows its type and how to convert to a query
     * @param topK Maximum number of results per search type
     * @return Combined results from all search types
     */
    suspend fun search(
        searchCriteria: List<SearchCriteria>,
        topK: Int
    ): MultiSearchResult

    /**
     * Get the name of this strategy (for logging/debugging)
     */
    fun getStrategyName(): String
}
