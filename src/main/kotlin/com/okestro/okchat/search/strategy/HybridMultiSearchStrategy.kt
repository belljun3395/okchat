package com.okestro.okchat.search.strategy

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
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Hybrid multi-search strategy using batched requests.
 *
 * Responsibility:
 * - Execute multiple search types (keyword/title/content/path) in a single batched request
 * - Generate embeddings once and reuse across all searches
 * - Parse and deduplicate results for each search type
 *
 * Benefits:
 * - Performance: Single HTTP request instead of 4 separate requests
 * - Efficiency: Embedding generated once and reused
 * - Consistency: All searches use the same embedding
 */
@Component
class HybridMultiSearchStrategy(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel,
    private val fieldConfig: SearchFieldWeightConfig
) : MultiSearchStrategy {

    /**
     * Data class to hold search request information
     */
    private data class SearchRequestInfo(
        val type: SearchType,
        val request: HybridSearchRequest,
        val index: Int
    )

    override suspend fun executeMultiSearch(
        keywords: SearchKeywords?,
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        topK: Int
    ): MultiSearchResult {
        log.info {
            "[${getStrategyName()}] Starting with topK=$topK: " +
                "keywords=${keywords?.keywords?.size ?: 0}, titles=${titles?.titles?.size ?: 0}, " +
                "contents=${contents?.contents?.size ?: 0}, paths=${paths?.paths?.size ?: 0}"
        }

        // Generate embedding once (reused for all searches)
        val embedding = generateEmbedding(contents)

        // Build search requests
        val searchRequests = buildSearchRequests(keywords, titles, contents, paths, embedding, topK)

        // Execute batched search
        val responses = executeBatchedSearch(searchRequests)

        // Parse and wrap results
        val resultsByType = parseSearchResponses(searchRequests, responses)

        // Log results if debug enabled
        if (log.isDebugEnabled()) {
            logSearchResults(resultsByType)
        }

        return wrapResults(resultsByType)
    }

    /**
     * Generate embedding from content terms, reused across all searches
     */
    private suspend fun generateEmbedding(contents: SearchContents?): List<Float> {
        log.debug { "[${getStrategyName()}] Generating embedding..." }
        val embedding = embeddingModel.embed(
            contents?.contents?.joinToString { it.term } ?: ""
        ).toList()
        log.debug { "[${getStrategyName()}] Embedding generated with dimension ${embedding.size}" }
        return embedding
    }

    /**
     * Build search requests for all provided search parameters
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

        fun addRequest(type: SearchType, query: String?) {
            query?.let {
                val fieldWeights = type.getFieldWeights(fieldConfig)
                requests.add(
                    SearchRequestInfo(
                        type = type,
                        request = HybridSearchRequest(
                            textQuery = it,
                            vectorQuery = embedding,
                            fields = SearchFields(
                                queryBy = fieldWeights.queryByList(),
                                weights = fieldWeights.weightsList()
                            ),
                            filters = mapOf("metadata.type" to "confluence-page"),
                            limit = topK
                        ),
                        index = index++
                    )
                )
            }
        }

        addRequest(SearchType.KEYWORD, keywords?.toOrQuery())
        addRequest(SearchType.TITLE, titles?.toOrQuery())
        addRequest(SearchType.CONTENT, contents?.toOrQuery())
        addRequest(SearchType.PATH, paths?.toOrQuery())

        return requests
    }

    /**
     * Execute all searches in a single batched HTTP request
     */
    private suspend fun executeBatchedSearch(
        searchRequests: List<SearchRequestInfo>
    ): List<HybridSearchResponse> {
        log.debug { "[${getStrategyName()}] Executing batched search with ${searchRequests.size} requests..." }
        return searchClient.multiHybridSearch(searchRequests.map { it.request })
    }

    /**
     * Parse and deduplicate search responses by type
     */
    private fun parseSearchResponses(
        requests: List<SearchRequestInfo>,
        responses: List<HybridSearchResponse>
    ): Map<SearchType, List<SearchResult>> {
        return requests.associate { requestInfo ->
            log.debug { "[${getStrategyName()}] Parsing ${requestInfo.type.getDisplayName()} results..." }

            val results = HybridSearchUtils.deduplicateResults(
                HybridSearchUtils.parseSearchResults(responses[requestInfo.index])
            )

            requestInfo.type to results
        }
    }

    /**
     * Wrap parsed results in type-safe result classes
     */
    private fun wrapResults(resultsByType: Map<SearchType, List<SearchResult>>): MultiSearchResult {
        return MultiSearchResult(
            keywordResults = KeywordSearchResults(resultsByType[SearchType.KEYWORD] ?: emptyList()),
            titleResults = TitleSearchResults(resultsByType[SearchType.TITLE] ?: emptyList()),
            contentResults = ContentSearchResults(resultsByType[SearchType.CONTENT] ?: emptyList()),
            pathResults = PathSearchResults(resultsByType[SearchType.PATH] ?: emptyList())
        )
    }

    /**
     * Log top 5 results for each search type (debug only)
     */
    private fun logSearchResults(resultsByType: Map<SearchType, List<SearchResult>>) {
        resultsByType.forEach { (type, results) ->
            val prefix = type.name.first() // K, T, C, P
            log.debug { "[${getStrategyName()}] ━━━ ${type.getDisplayName()} search top 5 ━━━" }
            results.take(5).forEachIndexed { i, r ->
                log.debug {
                    "  [$prefix${i + 1}] ${r.title} " +
                        "(score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)"
                }
            }
        }
        log.debug { "[${getStrategyName()}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
    }

    override fun getStrategyName(): String = "HybridMultiSearch"
}
