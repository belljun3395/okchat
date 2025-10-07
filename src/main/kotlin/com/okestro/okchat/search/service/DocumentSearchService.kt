package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.strategy.MultiSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Document search service that delegates to a pluggable search strategy.
 *
 * Responsibility:
 * - Provide a stable API for multi-search operations
 * - Delegate actual search execution to a configurable strategy
 * - Log search operations at the service layer
 *
 * Benefits of Strategy pattern:
 * - Flexibility: Easy to switch between different search implementations
 *   (e.g., HybridSearch, SequentialSearch, ParallelSearch)
 * - Testability: Strategies can be tested independently
 * - Maintainability: Search logic is isolated in strategy classes
 * - Clarity: Service has single responsibility (delegation), strategy has single responsibility (execution)
 */
@Service
class DocumentSearchService(
    private val searchStrategy: MultiSearchStrategy
) {

    /**
     * Perform multi-search across titles, contents, paths, and keywords.
     * Delegates to the configured search strategy.
     *
     * @param titles Title search terms
     * @param contents Content search terms
     * @param paths Path search terms
     * @param keywords Keyword search terms
     * @param topK Maximum results per search type
     * @return Combined multi-search results
     */
    suspend fun multiSearch(
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        keywords: SearchKeywords?,
        topK: Int = 50
    ): MultiSearchResult {
        log.info { "[DocumentSearchService] Delegating to ${searchStrategy.getStrategyName()} strategy" }

        return searchStrategy.executeMultiSearch(
            keywords = keywords,
            titles = titles,
            contents = contents,
            paths = paths,
            topK = topK
        )
    }
}
