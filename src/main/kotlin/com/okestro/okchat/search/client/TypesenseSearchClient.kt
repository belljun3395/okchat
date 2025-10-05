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
        log.debug { "[Typesense] Executing search: q='${request.q}'" }

        return webClient.post()
            .uri("/collections/$collectionName/documents/search")
            .bodyValue(request)
            .retrieve()
            .onStatus({ it.isError }) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).map { body ->
                    log.error { "[Typesense] Search error: $body" }
                    RuntimeException("Typesense search failed: $body")
                }
            }
            .bodyToMono(TypesenseSearchResponse::class.java)
            .doOnError { error ->
                log.error(error) { "[Typesense] Request failed: ${error.message}" }
            }
            .awaitSingle()
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
