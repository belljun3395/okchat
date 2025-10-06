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
     * Normalize Typesense text_match score to 0-1 range using log-based scaling.
     *
     * Typesense text_match is a BM25-based relevance score that can range from 0 to several hundred or more.
     * We use a logarithmic normalization for better distribution across the [0, 1] range.
     *
     * Formula: log(1 + score) / log(1 + max_expected_score)
     * - Assumes max typical score around 500 (99th percentile)
     * - Scores above this will be capped at 1.0
     * - Provides better differentiation for lower scores
     *
     * Example mappings:
     * - score 0 -> 0.0
     * - score 10 -> ~0.39
     * - score 50 -> ~0.65
     * - score 100 -> ~0.74
     * - score 250 -> ~0.88
     * - score 500 -> ~1.0
     */
    private fun normalizeTextMatch(textMatch: Long?): Double {
        if (textMatch == null || textMatch == 0L) return 0.0

        val score = textMatch.toDouble()
        val maxExpectedScore = 500.0 // Configurable based on your data distribution
        val normalizedScore = kotlin.math.ln(1.0 + score) / kotlin.math.ln(1.0 + maxExpectedScore)

        return kotlin.math.min(1.0, normalizedScore)
    }

    /**
     * Normalize vector distance to similarity score (0-1 range).
     *
     * Vector distance measures dissimilarity (lower = more similar).
     * We convert this to a similarity score using the formula: 1 / (1 + distance)
     *
     * This exponential decay provides:
     * - distance 0.0 -> similarity 1.0 (identical)
     * - distance 0.5 -> similarity 0.67
     * - distance 1.0 -> similarity 0.5
     * - distance 2.0 -> similarity 0.33
     * - distance ∞ -> similarity 0.0
     */
    private fun normalizeVectorDistance(vectorDistance: Double?): Double {
        if (vectorDistance == null) return 0.0
        return 1.0 / (1.0 + vectorDistance)
    }
}

/**
 * Convert HybridSearchRequest to TypesenseSearchRequest with alpha parameter for hybrid search weighting.
 *
 * Alpha parameter controls text vs vector search weight:
 * - alpha = 1.0: 100% text search
 * - alpha = 0.0: 100% vector search
 * - alpha = 0.6: 60% text, 40% vector
 */
private fun HybridSearchRequest.toTypesenseRequest() = TypesenseSearchRequest(
    q = textQuery,
    queryBy = fields.queryBy.joinToString(","),
    queryByWeights = fields.weights.joinToString(","),
    vectorQuery = "embedding:([${vectorQuery.joinToString(",")}], k:$limit, alpha:$textWeight)",
    filterBy = filters.entries.joinToString(" && ") { "${it.key}:=${it.value}" }
        .takeIf { it.isNotEmpty() },
    perPage = limit,
    page = 1
)
