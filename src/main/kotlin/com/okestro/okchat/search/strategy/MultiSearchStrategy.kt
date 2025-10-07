package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles

/**
 * Strategy interface for multi-search operations.
 *
 * Responsibility: Define the contract for executing multi-type searches
 *
 * Implementations can provide different search strategies:
 * - HybridMultiSearchStrategy: Batched hybrid search (current implementation)
 * - SequentialMultiSearchStrategy: Execute searches sequentially
 * - ParallelMultiSearchStrategy: Execute searches in parallel
 *
 * Benefits of Strategy pattern:
 * - Flexible: Easy to switch search strategies
 * - Testable: Each strategy can be tested independently
 * - Clear: Each strategy has a single, well-defined responsibility
 */
interface MultiSearchStrategy {

    /**
     * Execute multi-search across different search types.
     *
     * @param keywords Keyword-based search terms
     * @param titles Title-based search terms
     * @param contents Content-based search terms
     * @param paths Path/location-based search terms
     * @param topK Maximum number of results per search type
     * @return Combined results from all search types
     */
    suspend fun executeMultiSearch(
        keywords: SearchKeywords?,
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        topK: Int
    ): MultiSearchResult

    /**
     * Get the name of this strategy (for logging/debugging)
     */
    fun getStrategyName(): String
}
