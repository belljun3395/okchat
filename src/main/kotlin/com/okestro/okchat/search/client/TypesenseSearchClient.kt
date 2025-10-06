package com.okestro.okchat.search.client

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import org.typesense.api.Client
import org.typesense.api.exceptions.TypesenseError
import org.typesense.model.MultiSearchCollectionParameters
import org.typesense.model.MultiSearchResult
import org.typesense.model.MultiSearchSearchesParameter
import org.typesense.model.SearchParameters

private val log = KotlinLogging.logger {}

/**
 * Typesense search client using official Java Client with RetryTemplate for resilience.
 * Provides type-safe API with full access to Typesense search features.
 */
@Component
class TypesenseSearchClient(
    private val typesenseClient: Client,
    private val retryTemplate: RetryTemplate,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String
) {

    suspend fun search(request: TypesenseSearchRequest): TypesenseSearchResponse {
        log.debug { "[Typesense] Request: q='${request.q}', queryBy='${request.queryBy}', vectorQuery='${request.vectorQuery?.take(100)}...'" }

        return withContext(Dispatchers.IO) {
            try {
                retryTemplate.execute<TypesenseSearchResponse, TypesenseError> { context ->
                    if (context.retryCount > 0) {
                        log.warn { "[Typesense] Retry attempt ${context.retryCount} for query: ${request.q}" }
                    }

                    val searchParams = request.toSearchParameters()
                    val searchResult = typesenseClient.collections(collectionName)
                        .documents()
                        .search(searchParams)

                    TypesenseSearchResponse(
                        hits = searchResult.hits?.map { hit ->
                            TypesenseHit(
                                document = hit.document,
                                textMatch = hit.textMatch,
                                vectorDistance = hit.vectorDistance?.toDouble()
                            )
                        } ?: emptyList(),
                        found = searchResult.found
                    )
                }
            } catch (e: TypesenseError) {
                log.error(e) { "[Typesense] Search failed for q='${request.q}': ${e.message}" }
                throw RuntimeException("Typesense search failed: ${e.message}", e)
            } catch (e: Exception) {
                log.error(e) { "[Typesense] Unexpected error: ${e.message}" }
                throw e
            }
        }
    }

    /**
     * Execute multiple searches in a single HTTP request using official multi-search API.
     * This dramatically reduces network latency compared to sequential searches.
     */
    suspend fun multiSearch(requests: List<TypesenseSearchRequest>): List<TypesenseSearchResponse> {
        if (requests.isEmpty()) return emptyList()

        log.debug { "[Typesense] Multi-search with ${requests.size} queries" }

        return withContext(Dispatchers.IO) {
            try {
                retryTemplate.execute<List<TypesenseSearchResponse>, TypesenseError> { context ->
                    if (context.retryCount > 0) {
                        log.warn { "[Typesense] Multi-search retry attempt ${context.retryCount}" }
                    }

                    val searchesParam = requests.map { request ->
                        // Create MultiSearchCollectionParameters for each request
                        request.toMultiSearchParameters(collectionName)
                    }.let {
                        // Wrap in MultiSearchSearchesParameter
                        MultiSearchSearchesParameter()
                            .searches(it)
                    }

                    // Execute multi-search
                    val multiSearchResult: MultiSearchResult = typesenseClient.multiSearch.perform(
                        searchesParam,
                        HashMap()
                    )

                    // Convert results
                    multiSearchResult.results?.map { result ->
                        TypesenseSearchResponse(
                            hits = result.hits?.map { hit ->
                                TypesenseHit(
                                    document = hit.document,
                                    textMatch = hit.textMatch,
                                    vectorDistance = hit.vectorDistance?.toDouble()
                                )
                            } ?: emptyList(),
                            found = result.found
                        )
                    } ?: emptyList()
                }
            } catch (e: TypesenseError) {
                log.error(e) { "[Typesense] Multi-search failed: ${e.message}" }
                throw RuntimeException("Typesense multi-search failed: ${e.message}", e)
            } catch (e: Exception) {
                log.error(e) { "[Typesense] Multi-search unexpected error: ${e.message}" }
                throw e
            }
        }
    }
}

data class TypesenseSearchRequest(
    val q: String,
    @JsonProperty("query_by") val queryBy: String,
    @JsonProperty("query_by_weights") val queryByWeights: String? = null,
    @JsonProperty("vector_query") val vectorQuery: String? = null,
    @JsonProperty("filter_by") val filterBy: String? = null,
    @JsonProperty("per_page") val perPage: Int = 10,
    val page: Int = 1,
    val prefix: Boolean = true, // Enable prefix matching (e.g., "2508" matches "250804")
    @JsonProperty("num_typos") val numTypos: Int = 2, // Allow fuzzy matching with 2 typos
    @JsonProperty("typo_tokens_threshold") val typoTokensThreshold: Int = 0 // Apply typos to all tokens
)

/**
 * Convert to official Typesense SearchParameters for single search.
 * Note: Type conversions applied for API compatibility:
 * - prefix: Boolean → String
 * - numTypos: Int → String
 * - typoTokensThreshold: Int → Integer
 */
fun TypesenseSearchRequest.toSearchParameters(): SearchParameters {
    return SearchParameters()
        .q(q)
        .queryBy(queryBy)
        .apply { queryByWeights?.let { queryByWeights(it) } }
        .apply { vectorQuery?.let { vectorQuery(it) } }
        .apply { filterBy?.let { filterBy(it) } }
        .perPage(perPage)
        .page(page)
        .prefix(prefix.toString()) // Boolean → String conversion
        .numTypos(numTypos.toString()) // Int → String conversion
        .typoTokensThreshold(typoTokensThreshold) // Int → Integer (auto-boxing)
}

/**
 * Convert to MultiSearchCollectionParameters for multi-search.
 * Extends MultiSearchParameters with collection specification.
 *
 * Note: Using setters due to builder pattern returning parent type
 */
fun TypesenseSearchRequest.toMultiSearchParameters(collection: String): MultiSearchCollectionParameters {
    val params = MultiSearchCollectionParameters()

    params.collection = collection
    params.q = q
    params.queryBy = queryBy
    params.perPage = perPage
    params.page = page
    params.prefix = prefix.toString() // Boolean → String conversion
    params.numTypos = numTypos.toString() // Int → String conversion
    params.typoTokensThreshold = typoTokensThreshold // Int → Integer (auto-boxing)

    // Apply optional parameters
    queryByWeights?.let { params.queryByWeights = it }
    vectorQuery?.let { params.vectorQuery = it }
    filterBy?.let { params.filterBy = it }

    return params
}

data class TypesenseSearchResponse(
    val hits: List<TypesenseHit>? = null,
    val found: Int? = null
)

data class TypesenseHit(
    val document: Map<String, Any>? = null,
    @JsonProperty("text_match") val textMatch: Long? = null,
    @JsonProperty("vector_distance") val vectorDistance: Double? = null
)
