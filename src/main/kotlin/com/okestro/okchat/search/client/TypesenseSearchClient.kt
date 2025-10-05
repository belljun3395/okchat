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

        val multiSearchRequest = mapOf(
            "searches" to listOf(
                mapOf(
                    "collection" to collectionName,
                    "q" to request.q,
                    "query_by" to request.queryBy,
                    "query_by_weights" to request.queryByWeights,
                    "vector_query" to request.vectorQuery,
                    "filter_by" to request.filterBy,
                    "per_page" to request.perPage,
                    "page" to request.page
                ).filterValues { it != null }
            )
        )

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
            .bodyToMono(Map::class.java)
            .doOnError { error ->
                log.error(error) { "[Typesense] Request failed: ${error.message}" }
            }
            .awaitSingle()

        @Suppress("UNCHECKED_CAST")
        val results = multiSearchResponse["results"] as? List<Map<String, Any>> ?: emptyList()
        val firstResult = results.firstOrNull() ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val hits = (firstResult["hits"] as? List<Map<String, Any>>)?.map { hit ->
            TypesenseHit(
                document = hit["document"] as? Map<String, Any>,
                textMatch = (hit["text_match"] as? Number)?.toLong(),
                vectorDistance = (hit["vector_distance"] as? Number)?.toDouble()
            )
        } ?: emptyList()

        return TypesenseSearchResponse(
            hits = hits,
            found = (firstResult["found"] as? Number)?.toInt()
        )
    }

    /**
     * Execute multiple searches in a single HTTP request
     * This is the REAL multi-search - dramatically reduces network latency
     */
    suspend fun multiSearch(requests: List<TypesenseSearchRequest>): List<TypesenseSearchResponse> {
        if (requests.isEmpty()) return emptyList()

        log.debug { "[Typesense] Multi-search with ${requests.size} queries" }

        val multiSearchRequest = mapOf(
            "searches" to requests.map { request ->
                mapOf(
                    "collection" to collectionName,
                    "q" to request.q,
                    "query_by" to request.queryBy,
                    "query_by_weights" to request.queryByWeights,
                    "vector_query" to request.vectorQuery,
                    "filter_by" to request.filterBy,
                    "per_page" to request.perPage,
                    "page" to request.page
                ).filterValues { it != null }
            }
        )

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
            .bodyToMono(Map::class.java)
            .doOnError { error ->
                log.error(error) { "[Typesense] Multi-search request failed: ${error.message}" }
            }
            .awaitSingle()

        @Suppress("UNCHECKED_CAST")
        val results = multiSearchResponse["results"] as? List<Map<String, Any>> ?: emptyList()

        return results.map { result ->
            @Suppress("UNCHECKED_CAST")
            val hits = (result["hits"] as? List<Map<String, Any>>)?.map { hit ->
                TypesenseHit(
                    document = hit["document"] as? Map<String, Any>,
                    textMatch = (hit["text_match"] as? Number)?.toLong(),
                    vectorDistance = (hit["vector_distance"] as? Number)?.toDouble()
                )
            } ?: emptyList()

            TypesenseSearchResponse(
                hits = hits,
                found = (result["found"] as? Number)?.toInt()
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
    val page: Int = 1
)

data class TypesenseSearchResponse(
    val hits: List<TypesenseHit>? = null,
    val found: Int? = null
)

data class TypesenseHit(
    val document: Map<String, Any>? = null,
    @JsonProperty("text_match") val textMatch: Long? = null,
    @JsonProperty("vector_distance") val vectorDistance: Double? = null
)
