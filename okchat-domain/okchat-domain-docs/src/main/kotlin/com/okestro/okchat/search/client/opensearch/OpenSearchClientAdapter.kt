package com.okestro.okchat.search.client.opensearch

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchHit
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.math.exp

private val log = KotlinLogging.logger {}

/**
 * Adapter Pattern: Translates generic SearchClient requests to OpenSearch-specific API.
 * * Responsibilities:
 * - Convert HybridSearchRequest to OpenSearchSearchRequest
 * - Normalize scores from OpenSearch responses
 * - Handle multi-search batching
 * * This adapter decouples the application from OpenSearch-specific details,
 * allowing easy switching between search engines.
 */
@Component
class OpenSearchClientAdapter(
    private val openSearchClient: OpenSearchSearchClient
) : SearchClient {

    override suspend fun hybridSearch(request: HybridSearchRequest): HybridSearchResponse {
        log.debug { "[OpenSearch Adapter] Executing search: q='${request.textQuery}'" }

        val openSearchResponse = openSearchClient.search(request.toOpenSearchRequest())

        val hits = openSearchResponse.hits.map { hit ->
            // OpenSearch returns a combined score, we need to separate text and vector scores
            // For hybrid search, we estimate the breakdown based on textWeight
            val totalScore = normalizeScore(hit.score)
            val textScore = totalScore * request.textWeight
            val vectorScore = totalScore * (1 - request.textWeight)

            log.trace { "[Score] total=${hit.score} -> normalized=$totalScore (text=$textScore, vector=$vectorScore)" }

            SearchHit(
                document = hit.document,
                textScore = textScore,
                vectorScore = vectorScore
            )
        }

        log.debug { "[OpenSearch Adapter] Returned ${hits.size} hits" }

        return HybridSearchResponse(hits)
    }

    override suspend fun multiHybridSearch(requests: List<HybridSearchRequest>): List<HybridSearchResponse> {
        if (requests.isEmpty()) return emptyList()

        log.info { "[OpenSearch Adapter] Executing multi-search with ${requests.size} queries" }
        requests.forEachIndexed { index, req ->
            log.info { "[OpenSearch Adapter] Request ${index + 1}: textQuery='${req.textQuery}', fields=${req.fields.queryBy}, limit=${req.limit}" }
        }

        // Convert all HybridSearchRequests to OpenSearchSearchRequests
        val openSearchRequests = requests.map { it.toOpenSearchRequest() }

        // Execute all searches in a single HTTP request
        val openSearchResponses = openSearchClient.multiSearch(openSearchRequests)

        // Convert back to HybridSearchResponses
        return openSearchResponses.map { openSearchResponse ->
            val hits = openSearchResponse.hits.map { hit ->
                val totalScore = normalizeScore(hit.score)

                SearchHit(
                    document = hit.document,
                    textScore = totalScore * 0.5, // Default split for multi-search
                    vectorScore = totalScore * 0.5
                )
            }

            HybridSearchResponse(hits)
        }
    }

    /**
     * Normalize OpenSearch score to 0-1 range using sigmoid function.
     * * OpenSearch returns relevance scores that can vary widely depending on
     * the query and document characteristics. We use a sigmoid function to
     * normalize these scores to a consistent [0, 1] range.
     * * Formula: 1 / (1 + e^(-score/k))
     * where k is a scaling factor (default: 5.0)
     * * This provides:
     * - score 0 -> ~0.5
     * - score 5 -> ~0.73
     * - score 10 -> ~0.88
     * - score 20 -> ~0.98
     */
    private fun normalizeScore(score: Double): Double {
        if (score <= 0.0) return 0.0

        val k = 5.0 // Scaling factor - adjust based on your score distribution
        return 1.0 / (1.0 + exp(-score / k))
    }
}

/**
 * Convert HybridSearchRequest to OpenSearchSearchRequest.
 * Maps the generic search parameters to OpenSearch-specific format.
 */
private fun HybridSearchRequest.toOpenSearchRequest() = OpenSearchSearchRequest(
    textQuery = textQuery,
    fields = fields.queryBy,
    weights = fields.weights,
    vectorQuery = vectorQuery,
    filterBy = filters,
    limit = limit,
    textWeight = textWeight
)
