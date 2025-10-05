package com.okestro.okchat.search.client

/**
 * Search engine abstraction.
 * Allows switching between Typesense, Elasticsearch, OpenSearch, etc.
 */
interface SearchClient {
    suspend fun hybridSearch(request: HybridSearchRequest): HybridSearchResponse
}

data class HybridSearchRequest(
    val textQuery: String,
    val vectorQuery: List<Float>,
    val fields: SearchFields,
    val filters: Map<String, String> = emptyMap(),
    val limit: Int = 10
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
