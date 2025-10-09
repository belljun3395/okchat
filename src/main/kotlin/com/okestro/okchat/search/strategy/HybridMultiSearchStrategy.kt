package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.MetadataFields
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchCriteria
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.model.TypedSearchResults
import com.okestro.okchat.search.util.HybridSearchUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Hybrid multi-search strategy using batched requests.
 *
 * Responsibility:
 * - Execute multiple search types in a single batched request
 * - Generate embeddings once and reuse across all searches
 * - Parse and deduplicate results for each search type
 * - Support polymorphic search criteria (Open-Closed Principle)
 *
 * Benefits:
 * - Performance: Single HTTP request instead of N separate requests
 * - Efficiency: Embedding generated once and reused
 * - Consistency: All searches use the same embedding
 * - Extensibility: New search types supported without code changes
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

    override suspend fun search(
        searchCriteria: List<SearchCriteria>,
        topK: Int
    ): MultiSearchResult {
        // Filter out empty criteria
        val validCriteria = searchCriteria.filterNot { it.isEmpty() }

        log.info {
            "[${getStrategyName()}] Starting with topK=$topK, ${validCriteria.size} active criteria: " +
                validCriteria.joinToString(", ") { "${it.getSearchType().name}(${it.size()})" }
        }

        if (validCriteria.isEmpty()) {
            log.warn { "[${getStrategyName()}] No valid search criteria provided" }
            return MultiSearchResult.empty()
        }

        // Generate embedding once (reused for all searches)
        val embedding = generateEmbeddingFromCriteria(validCriteria)

        // Build search requests from criteria
        val searchRequests = buildSearchRequestsFromCriteria(validCriteria, embedding, topK)

        log.debug { "[${getStrategyName()}] Executing batched search with ${searchRequests.size} requests..." }
        val responses = searchClient.multiHybridSearch(searchRequests.map { it.request })

        // Parse and wrap results
        val resultsByType = parseSearchResponses(searchRequests, responses)

        // Log results if debug enabled
        if (log.isDebugEnabled()) {
            logSearchResults(resultsByType)
        }

        return wrapResults(resultsByType)
    }

    /**
     * Generate embedding from content criteria, reused across all searches.
     *
     * IMPORTANT: Only uses Content criteria for embedding to maintain vector space consistency.
     * - Documents in vector DB are indexed with content embeddings
     * - Using title/keyword for embedding would cause vector space mismatch
     * - If no content criteria: returns empty embedding (falls back to BM25-only search)
     *
     * Search behavior:
     * - With content criteria: Hybrid search (BM25 + Vector similarity)
     * - Without content criteria: BM25 only (text matching)
     */
    private suspend fun generateEmbeddingFromCriteria(criteria: List<SearchCriteria>): List<Float> {
        log.debug { "[${getStrategyName()}] Generating embedding..." }

        // Only use content criteria for embedding (vector space consistency)
        val embeddingText = criteria
            .firstOrNull { it.getSearchType() == SearchType.CONTENT }
            ?.toQuery()

        val embedding = embeddingText?.let {
            embeddingModel.embed(it).toList()
        } ?: emptyList() // No content = BM25 only (correct behavior)

        log.debug {
            "[${getStrategyName()}] Embedding generated with dimension ${embedding.size} " +
                "(mode: ${if (embedding.isEmpty()) "BM25-only" else "Hybrid"})"
        }
        return embedding
    }

    /**
     * Build search requests from search criteria (polymorphic approach).
     * Each criterion knows its type and converts itself to a query.
     */
    private fun buildSearchRequestsFromCriteria(
        criteria: List<SearchCriteria>,
        embedding: List<Float>,
        topK: Int
    ): List<SearchRequestInfo> {
        return criteria.mapIndexed { index, criterion ->
            val type = criterion.getSearchType()
            val fieldWeights = type.getFieldWeights(fieldConfig)

            SearchRequestInfo(
                type = type,
                request = HybridSearchRequest(
                    textQuery = criterion.toQuery(),
                    vectorQuery = embedding,
                    fields = SearchFields(
                        queryBy = fieldWeights.queryByList(),
                        weights = fieldWeights.weightsList()
                    ),
                    filters = mapOf(MetadataFields.TYPE to "confluence-page"),
                    limit = topK
                ),
                index = index
            )
        }
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
     * Wrap parsed results in type-safe result classes.
     * Uses TypedSearchResults factory for polymorphic creation.
     */
    private fun wrapResults(resultsByType: Map<SearchType, List<SearchResult>>): MultiSearchResult {
        val typedResults = resultsByType.mapValues { (type, results) ->
            TypedSearchResults.of(type, results)
        }
        return MultiSearchResult.fromMap(typedResults)
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
