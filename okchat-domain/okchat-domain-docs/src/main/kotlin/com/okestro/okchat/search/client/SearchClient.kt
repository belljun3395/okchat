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
     * Alpha parameter for hybrid search weighting (0.0 to 1.0).
     * Typesense default: 0.3 (30% keyword, 70% semantic)
     *
     * - alpha = 1.0: 100% keyword search (no semantic)
     * - alpha = 0.7: 70% keyword, 30% semantic
     * - alpha = 0.3: 30% keyword, 70% semantic (Typesense recommended)
     * - alpha = 0.0: 100% semantic search (no keyword)
     *
     * Lower values favor semantic similarity, higher values favor keyword matching.
     */
    val textWeight: Double = 0.3 // Typesense 29.0 recommended default
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
