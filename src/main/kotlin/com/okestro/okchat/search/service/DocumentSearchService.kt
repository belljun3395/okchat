package com.okestro.okchat.search.service

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.ContentSearchResults
import com.okestro.okchat.search.model.KeywordSearchResults
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.PathSearchResults
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.model.TitleSearchResults
import com.okestro.okchat.search.util.HybridSearchUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Document search service optimized for multi-search performance.
 * Uses HybridSearchUtils for common search logic.
 * Refactored to eliminate code duplication using SearchType enum.
 */
@Service
class DocumentSearchService(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel,
    private val fieldConfig: SearchFieldWeightConfig
) {

    /**
     * Data class to hold search request information
     */
    private data class SearchRequestInfo(
        val type: SearchType,
        val request: HybridSearchRequest,
        val index: Int
    )

    /**
     * Perform multi-search across titles, contents, paths, and keywords
     * Combines results from different search types and deduplicates
     */
    suspend fun multiSearch(
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        keywords: SearchKeywords?,
        topK: Int = 50
    ): MultiSearchResult {
        log.info {
            "[Multi-Search] Starting multi-search with topK=$topK: " +
                "titles=${titles?.titles?.size ?: 0}, contents=${contents?.contents?.size ?: 0}, " +
                "paths=${paths?.paths?.size ?: 0}, keywords=${keywords?.keywords?.size ?: 0}"
        }

        // Generate embedding once (reused for all searches)
        log.debug { "[Multi-Search] Generating embedding..." }
        val embedding = embeddingModel.embed(
            contents?.contents?.joinToString { it.term } ?: ""
        ).toList()
        log.debug { "[Multi-Search] Embedding generated with dimension ${embedding.size}" }

        // Build search requests using unified approach
        val searchRequests = buildSearchRequests(
            keywords = keywords,
            titles = titles,
            contents = contents,
            paths = paths,
            embedding = embedding,
            topK = topK
        )

        // Execute all searches in a single HTTP request
        log.debug { "[Multi-Search] Executing batched search with ${searchRequests.size} requests..." }
        val responses = searchClient.multiHybridSearch(searchRequests.map { it.request })

        // Parse and deduplicate results by type
        val resultsByType = parseSearchResponses(searchRequests, responses)

        // Log results if debug enabled
        if (log.isDebugEnabled()) {
            logSearchResults(resultsByType)
        }

        return MultiSearchResult(
            keywordResults = KeywordSearchResults(resultsByType[SearchType.KEYWORD] ?: emptyList()),
            titleResults = TitleSearchResults(resultsByType[SearchType.TITLE] ?: emptyList()),
            contentResults = ContentSearchResults(resultsByType[SearchType.CONTENT] ?: emptyList()),
            pathResults = PathSearchResults(resultsByType[SearchType.PATH] ?: emptyList())
        )
    }

    /**
     * Build search requests for all provided search parameters.
     * Eliminates duplication by using SearchType enum.
     */
    private fun buildSearchRequests(
        keywords: SearchKeywords?,
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        embedding: List<Float>,
        topK: Int
    ): List<SearchRequestInfo> {
        val requests = mutableListOf<SearchRequestInfo>()
        var index = 0

        // Helper function to build request
        fun addRequest(type: SearchType, query: String?) {
            query?.let {
                requests.add(
                    SearchRequestInfo(
                        type = type,
                        request = HybridSearchRequest(
                            textQuery = it,
                            vectorQuery = embedding,
                            fields = SearchFields(
                                queryBy = type.getFieldWeights(fieldConfig).queryByList(),
                                weights = type.getFieldWeights(fieldConfig).weightsList()
                            ),
                            filters = mapOf("metadata.type" to "confluence-page"),
                            limit = topK
                        ),
                        index = index++
                    )
                )
            }
        }

        // Add requests for each search type
        addRequest(SearchType.KEYWORD, keywords?.toOrQuery())
        addRequest(SearchType.TITLE, titles?.toOrQuery())
        addRequest(SearchType.CONTENT, contents?.toOrQuery())
        addRequest(SearchType.PATH, paths?.toOrQuery())

        return requests
    }

    /**
     * Parse and deduplicate search responses by type.
     * Eliminates duplication in result parsing logic.
     */
    private fun parseSearchResponses(
        requests: List<SearchRequestInfo>,
        responses: List<HybridSearchResponse>
    ): Map<SearchType, List<SearchResult>> {
        return requests.associate { requestInfo ->
            log.debug { "[Multi-Search] Parsing ${requestInfo.type.getDisplayName()} results..." }

            val results = HybridSearchUtils.deduplicateResults(
                HybridSearchUtils.parseSearchResults(responses[requestInfo.index])
            )

            requestInfo.type to results
        }
    }

    /**
     * Log top 5 results for each search type (debug only)
     */
    private fun logSearchResults(resultsByType: Map<SearchType, List<SearchResult>>) {
        resultsByType.forEach { (type, results) ->
            val prefix = type.name.first() // K, T, C, P
            log.debug { "[Multi-Search] ━━━ ${type.getDisplayName()} search top 5 ━━━" }
            results.take(5).forEachIndexed { i, r ->
                log.debug {
                    "  [$prefix${i + 1}] ${r.title} " +
                        "(score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)"
                }
            }
        }
        log.debug { "[Multi-Search] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
    }
}
