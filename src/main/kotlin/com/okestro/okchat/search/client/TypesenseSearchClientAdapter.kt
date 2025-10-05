package com.okestro.okchat.search.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Adapter Pattern: Translates generic search requests to Typesense-specific API.
 */
@Component
class TypesenseSearchClientAdapter(
    private val typesenseClient: TypesenseSearchClient
) : SearchClient {

    override suspend fun hybridSearch(request: HybridSearchRequest): HybridSearchResponse {
        log.debug { "[Typesense Adapter] Executing search: q='${request.textQuery}'" }

        val typesenseRequest = TypesenseSearchRequest(
            q = request.textQuery,
            queryBy = request.fields.queryBy.joinToString(","),
            queryByWeights = request.fields.weights.joinToString(","),
            vectorQuery = "embedding:([${request.vectorQuery.joinToString(",")}], k:${request.limit})",
            filterBy = request.filters.entries.joinToString(" && ") { "${it.key}:=${it.value}" }
                .takeIf { it.isNotEmpty() },
            perPage = request.limit,
            page = 1
        )

        val typesenseResponse = typesenseClient.search(typesenseRequest)

        val hits = typesenseResponse.hits?.map { hit ->
            SearchHit(
                document = hit.document ?: emptyMap(),
                textScore = (hit.textMatch?.toDouble() ?: 0.0) / 100.0,
                vectorScore = (hit.vectorDistance ?: 1.0).let { 1.0 / (1.0 + it) }
            )
        } ?: emptyList()

        return HybridSearchResponse(hits)
    }
}
