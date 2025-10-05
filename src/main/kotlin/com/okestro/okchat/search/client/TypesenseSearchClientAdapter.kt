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

        val typesenseResponse = typesenseClient.search(request.toTypesenseRequest())

        val hits = typesenseResponse.hits?.map { hit ->
            val normalizedTextScore = normalizeTextMatch(hit.textMatch)
            val normalizedVectorScore = normalizeVectorDistance(hit.vectorDistance)

            log.trace { "[Score] textMatch=${hit.textMatch} -> $normalizedTextScore, vectorDistance=${hit.vectorDistance} -> $normalizedVectorScore" }

            SearchHit(
                document = hit.document ?: emptyMap(),
                textScore = normalizedTextScore,
                vectorScore = normalizedVectorScore
            )
        } ?: emptyList()

        log.debug { "[Typesense Adapter] Returned ${hits.size} hits" }

        return HybridSearchResponse(hits)
    }

    override suspend fun multiHybridSearch(requests: List<HybridSearchRequest>): List<HybridSearchResponse> {
        if (requests.isEmpty()) return emptyList()

        log.info { "[Typesense Adapter] Executing multi-search with ${requests.size} queries" }
        requests.forEachIndexed { index, req ->
            log.info { "[Typesense Adapter] Request ${index + 1}: textQuery='${req.textQuery}', fields=${req.fields.queryBy}, limit=${req.limit}" }
        }

        // Convert all HybridSearchRequests to TypesenseSearchRequests
        val typesenseRequests = requests.map { it.toTypesenseRequest() }

        // Execute all searches in a single HTTP request
        val typesenseResponses = typesenseClient.multiSearch(typesenseRequests)

        // Convert back to HybridSearchResponses
        return typesenseResponses.map { typesenseResponse ->
            val hits = typesenseResponse.hits?.map { hit ->
                val normalizedTextScore = normalizeTextMatch(hit.textMatch)
                val normalizedVectorScore = normalizeVectorDistance(hit.vectorDistance)

                SearchHit(
                    document = hit.document ?: emptyMap(),
                    textScore = normalizedTextScore,
                    vectorScore = normalizedVectorScore
                )
            } ?: emptyList()

            HybridSearchResponse(hits)
        }
    }

    /**
     * Normalize Typesense text_match score to 0-1 range
     * text_match is a relevance score that can be very large (hundreds to thousands)
     * We use sigmoid-like function to normalize it
     */
    private fun normalizeTextMatch(textMatch: Long?): Double {
        if (textMatch == null || textMatch == 0L) return 0.0

        // Use log-based normalization to handle large values
        // This maps typical ranges (0-1000+) to approximately (0-1)
        val score = textMatch.toDouble()
        return kotlin.math.min(1.0, score / (score + 100.0))
    }

    /**
     * Normalize vector distance to similarity score (0-1 range)
     * Lower distance = higher similarity
     * Using 1/(1+d) formula which maps:
     * - distance 0 -> similarity 1.0
     * - distance 1 -> similarity 0.5 * - distance ∞ -> similarity 0.0
     */
    private fun normalizeVectorDistance(vectorDistance: Double?): Double {
        if (vectorDistance == null) return 0.0
        return 1.0 / (1.0 + vectorDistance)
    }
}

/**
 * Convert HybridSearchRequest to TypesenseSearchRequest
 */
private fun HybridSearchRequest.toTypesenseRequest() = TypesenseSearchRequest(
    q = textQuery,
    queryBy = fields.queryBy.joinToString(","),
    queryByWeights = fields.weights.joinToString(","),
    vectorQuery = "embedding:([${vectorQuery.joinToString(",")}], k:$limit)",
    filterBy = filters.entries.joinToString(" && ") { "${it.key}:=${it.value}" }
        .takeIf { it.isNotEmpty() },
    perPage = limit,
    page = 1
)
