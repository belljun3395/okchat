package com.okestro.okchat.search.client

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger {}

/**
 * Typesense HTTP client.
 */
@Component
class TypesenseSearchClient(
    @Value("\${spring.ai.vectorstore.typesense.client.protocol}") protocol: String,
    @Value("\${spring.ai.vectorstore.typesense.client.host}") host: String,
    @Value("\${spring.ai.vectorstore.typesense.client.port}") port: Int,
    @Value("\${spring.ai.vectorstore.typesense.client.apiKey}") apiKey: String,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String
) {
    private val webClient = WebClient.builder()
        .baseUrl("$protocol://$host:$port")
        .defaultHeader("X-TYPESENSE-API-KEY", apiKey)
        .defaultHeader("Content-Type", "application/json")
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        }
        .build()

    suspend fun search(request: TypesenseSearchRequest): TypesenseSearchResponse {
        log.debug { "[Typesense] Request: q='${request.q}', queryBy='${request.queryBy}', vectorQuery='${request.vectorQuery?.take(100)}...'" }

        val searchQuery = request.toSearchQuery(collectionName)
        val multiSearchRequest = TypesenseMultiSearchRequest(searches = listOf(searchQuery))

        val multiSearchResponse = webClient.post()
            .uri("/multi_search")
            .bodyValue(multiSearchRequest)
            .retrieve()
            .onStatus({ it.isError }) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    log.error { "[Typesense] Multi-search error for q='${request.q}': $body" }
                    RuntimeException("Typesense multi-search failed: $body")
                }
            }
            .bodyToMono(TypesenseMultiSearchResponse::class.java)
            .doOnError { error ->
                log.error(error) { "[Typesense] Request failed: ${error.message}" }
            }
            .awaitSingle()

        val firstResult = multiSearchResponse.results.firstOrNull()

        return TypesenseSearchResponse(
            hits = firstResult?.hits ?: emptyList(),
            found = firstResult?.found
        )
    }

    /**
     * Execute multiple searches in a single HTTP request
     * This is the REAL multi-search - dramatically reduces network latency
     */
    suspend fun multiSearch(requests: List<TypesenseSearchRequest>): List<TypesenseSearchResponse> {
        if (requests.isEmpty()) return emptyList()

        log.debug { "[Typesense] Multi-search with ${requests.size} queries" }

        val searchQueries = requests.map { it.toSearchQuery(collectionName) }
        val multiSearchRequest = TypesenseMultiSearchRequest(searches = searchQueries)

        val multiSearchResponse = webClient.post()
            .uri("/multi_search")
            .bodyValue(multiSearchRequest)
            .retrieve()
            .onStatus({ it.isError }) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    log.error { "[Typesense] Multi-search error: $body" }
                    RuntimeException("Typesense multi-search failed: $body")
                }
            }
            .bodyToMono(TypesenseMultiSearchResponse::class.java)
            .doOnError { error ->
                log.error(error) { "[Typesense] Multi-search request failed: ${error.message}" }
            }
            .awaitSingle()

        return multiSearchResponse.results.map { result ->
            TypesenseSearchResponse(
                hits = result.hits ?: emptyList(),
                found = result.found
            )
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
) {
    fun toSearchQuery(collection: String) = TypesenseSearchQuery(
        collection = collection,
        q = q,
        queryBy = queryBy,
        queryByWeights = queryByWeights,
        vectorQuery = vectorQuery,
        filterBy = filterBy,
        perPage = perPage,
        page = page,
        prefix = prefix,
        numTypos = numTypos,
        typoTokensThreshold = typoTokensThreshold
    )
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

/**
 * Individual search query within a multi-search request
 */
data class TypesenseSearchQuery(
    val collection: String,
    val q: String,
    @JsonProperty("query_by") val queryBy: String,
    @JsonProperty("query_by_weights") val queryByWeights: String? = null,
    @JsonProperty("vector_query") val vectorQuery: String? = null,
    @JsonProperty("filter_by") val filterBy: String? = null,
    @JsonProperty("per_page") val perPage: Int = 10,
    val page: Int = 1,
    val prefix: Boolean = true,
    @JsonProperty("num_typos") val numTypos: Int = 2,
    @JsonProperty("typo_tokens_threshold") val typoTokensThreshold: Int = 0
)

/**
 * Multi-search request wrapper
 */
data class TypesenseMultiSearchRequest(
    val searches: List<TypesenseSearchQuery>
)

/**
 * Individual search result within a multi-search response
 */
data class TypesenseSearchResult(
    val hits: List<TypesenseHit>? = null,
    val found: Int? = null
)

/**
 * Multi-search response wrapper
 */
data class TypesenseMultiSearchResponse(
    val results: List<TypesenseSearchResult>
)
