package com.okestro.okchat.search.client

/**
 * Search engine abstraction.
 * Allows switching between Typesense, Elasticsearch, OpenSearch, etc.
 */
interface SearchClient {
    suspend fun hybridSearch(request: HybridSearchRequest): HybridSearchResponse

    /**
     * Execute multiple hybrid searches in a single request
     * Dramatically reduces network latency by batching searches
     */
    suspend fun multiHybridSearch(requests: List<HybridSearchRequest>): List<HybridSearchResponse>
}

data class HybridSearchRequest(
    val textQuery: String,
    val vectorQuery: List<Float>,
    val fields: SearchFields,
    val filters: Map<String, String> = emptyMap(),
    val limit: Int = 10,
    /**
     * Text weight for hybrid search (0.0 to 1.0).
     * alpha = 1.0: 100% text search
     * alpha = 0.0: 100% vector search
     * alpha = 0.6: 60% text, 40% vector
     */
    val textWeight: Double = 0.5
)

data class SearchFields(
    val queryBy: List<String>,
    val weights: List<Int>
)

data class HybridSearchResponse(
    val hits: List<SearchHit>
)

data class SearchHit(
    val document: Map<String, Any>,
    val textScore: Double,
    val vectorScore: Double
)
