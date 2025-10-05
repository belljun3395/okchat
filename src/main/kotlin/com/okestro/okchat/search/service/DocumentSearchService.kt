package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.strategy.ContentSearchStrategy
import com.okestro.okchat.search.strategy.KeywordSearchStrategy
import com.okestro.okchat.search.strategy.TitleSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Simplified document search service using Strategy Pattern
 * Delegates search logic to specialized strategies
 */
@Service
class DocumentSearchService(
    private val keywordStrategy: KeywordSearchStrategy,
    private val titleStrategy: TitleSearchStrategy,
    private val contentStrategy: ContentSearchStrategy
) {

    /**
     * Search by keywords with hybrid search (text + vector)
     */
    suspend fun searchByKeywords(
        keywords: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> {
        log.info { "[Keyword Search] keywords='$keywords', topK=$topK" }

        return keywordStrategy.search(keywords, topK)
            .filter { it.score.value >= similarityThreshold }
    }

    /**
     * Search by title with hybrid search (text + vector)
     */
    suspend fun searchByTitle(
        query: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> {
        log.info { "[Title Search] query='$query', topK=$topK" }

        return titleStrategy.search(query, topK)
            .filter { it.score.value >= similarityThreshold }
    }

    /**
     * Search by content with hybrid search (text + vector)
     */
    suspend fun searchByContent(
        query: String,
        keywords: String = "",
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> {
        log.info { "[Content Search] query='$query', keywords='$keywords', topK=$topK" }

        // Use keywords if provided, otherwise use query
        val searchQuery = keywords.ifEmpty { query }

        return contentStrategy.search(searchQuery, topK)
            .filter { it.score.value >= similarityThreshold }
    }
}
