package com.okestro.okchat.search.client

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Low-level OpenSearch client using official Java Client with RetryTemplate.
 *
 * Responsibilities:
 * - Execute search requests against OpenSearch
 * - Handle retries and error recovery
 * - Provide type-safe API for search operations
 *
 * This class follows Single Responsibility Principle by focusing
 * solely on OpenSearch communication.
 */
@Component
class OpenSearchSearchClient(
    private val openSearchClient: OpenSearchClient,
    private val retryTemplate: RetryTemplate,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) {

    /**
     * Execute a single hybrid search (keyword + vector).
     * Note: Simplified to keyword-only search for now due to API complexity
     */
    suspend fun search(request: OpenSearchSearchRequest): OpenSearchSearchResponse {
        log.debug { "[OpenSearch] Request: q='${request.textQuery}', fields=${request.fields}" }

        return withContext(Dispatchers.IO) {
            try {
                retryTemplate.execute<OpenSearchSearchResponse, Exception> { context ->
                    if (context.retryCount > 0) {
                        log.warn { "[OpenSearch] Retry attempt ${context.retryCount} for query: ${request.textQuery}" }
                    }

                    val searchRequest = buildSearchRequest(request)
                    val searchResponse = openSearchClient.search(searchRequest, Map::class.java)

                    OpenSearchSearchResponse(
                        hits = searchResponse.hits().hits().map { hit ->
                            OpenSearchHit(
                                document = hit.source() as? Map<String, Any> ?: emptyMap(),
                                score = hit.score()?.toDouble() ?: 0.0,
                                index = hit.index()
                            )
                        },
                        total = searchResponse.hits().total()?.value() ?: 0
                    )
                }
            } catch (e: Exception) {
                log.error(e) { "[OpenSearch] Search failed for q='${request.textQuery}': ${e.message}" }
                throw RuntimeException("OpenSearch search failed: ${e.message}", e)
            }
        }
    }

    /**
     * Execute multiple searches sequentially.
     */
    suspend fun multiSearch(requests: List<OpenSearchSearchRequest>): List<OpenSearchSearchResponse> {
        if (requests.isEmpty()) return emptyList()

        log.debug { "[OpenSearch] Multi-search with ${requests.size} queries (sequential)" }

        return withContext(Dispatchers.IO) {
            requests.map { request ->
                try {
                    retryTemplate.execute<OpenSearchSearchResponse, Exception> { context ->
                        if (context.retryCount > 0) {
                            log.warn { "[OpenSearch] Retry attempt ${context.retryCount}" }
                        }

                        val searchRequest = buildSearchRequest(request)
                        val searchResponse = openSearchClient.search(searchRequest, Map::class.java)

                        OpenSearchSearchResponse(
                            hits = searchResponse.hits().hits().map { hit ->
                                OpenSearchHit(
                                    document = hit.source() as? Map<String, Any> ?: emptyMap(),
                                    score = hit.score()?.toDouble() ?: 0.0,
                                    index = hit.index()
                                )
                            },
                            total = searchResponse.hits().total()?.value() ?: 0
                        )
                    }
                } catch (e: Exception) {
                    log.error(e) { "[OpenSearch] Search failed: ${e.message}" }
                    // Return empty response on error
                    OpenSearchSearchResponse(emptyList(), 0)
                }
            }
        }
    }

    /**
     * Build OpenSearch SearchRequest with keyword search and filters.
     * TODO: Add hybrid search (keyword + k-NN) support
     */
    private fun buildSearchRequest(request: OpenSearchSearchRequest): SearchRequest {
        return SearchRequest.Builder()
            .index(indexName)
            .size(request.limit)
            .query { q ->
                if (request.filterBy.isNotEmpty()) {
                    // Apply filters with bool query
                    q.bool { b ->
                        b.must { m ->
                            m.multiMatch { mm ->
                                mm.query(request.textQuery)
                                    .fields(
                                        request.fields.mapIndexed { index, field ->
                                            "$field^${request.weights.getOrNull(index) ?: 1}"
                                        }
                                    )
                                    .type(TextQueryType.BestFields)
                                    .fuzziness("AUTO")
                            }
                        }
                        // Add each filter
                        val filters = request.filterBy.map { (field, value) ->
                            Query.of { qb ->
                                qb.term { t ->
                                    t.field(field)
                                        .value(FieldValue.of(value))
                                }
                            }
                        }
                        b.filter(filters)
                    }
                } else {
                    // No filters - simple query
                    q.multiMatch { mm ->
                        mm.query(request.textQuery)
                            .fields(
                                request.fields.mapIndexed { index, field ->
                                    "$field^${request.weights.getOrNull(index) ?: 1}"
                                }
                            )
                            .type(TextQueryType.BestFields)
                            .fuzziness("AUTO")
                    }
                }
            }
            .build()
    }
}

/**
 * OpenSearch search request model.
 */
data class OpenSearchSearchRequest(
    @JsonProperty("text_query") val textQuery: String,
    val fields: List<String>,
    val weights: List<Int> = emptyList(),
    @JsonProperty("vector_query") val vectorQuery: List<Float> = emptyList(),
    @JsonProperty("filter_by") val filterBy: Map<String, String> = emptyMap(),
    val limit: Int = 10,
    @JsonProperty("text_weight") val textWeight: Double = 0.5
)

/**
 * OpenSearch search response model.
 */
data class OpenSearchSearchResponse(
    val hits: List<OpenSearchHit>,
    val total: Long
)

/**
 * OpenSearch hit model.
 */
data class OpenSearchHit(
    val document: Map<String, Any>,
    val score: Double,
    val index: String?
)
