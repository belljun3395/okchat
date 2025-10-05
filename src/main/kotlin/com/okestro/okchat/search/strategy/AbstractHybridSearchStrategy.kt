package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel

private val log = KotlinLogging.logger {}

/**
 * Template Method Pattern for hybrid search strategies.
 * Engine-agnostic: works with any SearchClient implementation.
 */
abstract class AbstractHybridSearchStrategy(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel
) : SearchStrategy {

    override suspend fun search(query: String, topK: Int): List<SearchResult> {
        log.info { "[${getName()}] Searching: '$query' (topK=$topK)" }

        log.debug { "[${getName()}] Generating embedding..." }
        val embedding = embeddingModel.embed(query).toList()

        val request = buildSearchRequest(query, embedding, topK)

        log.debug { "[${getName()}] Executing search..." }
        val response = searchClient.hybridSearch(request)

        val hits = response.hits
        log.info { "[${getName()}] Found ${hits.size} documents" }

        val results = hits.map { hit ->
            val document = hit.document

            // Typesense stores metadata as flat keys with dot notation (metadata.title, etc.)
            val rawId = document["id"]?.toString() ?: ""
            val actualPageId = if (rawId.contains("_chunk_")) {
                rawId.substringBefore("_chunk_")
            } else {
                rawId
            }

            // Apply configured weights to combine text and vector scores
            val combinedScore = combineScores(hit.textScore, hit.vectorScore)
            val title = document["metadata.title"]?.toString() ?: "Untitled"
            log.trace { "[${getName()}] Result: id=$actualPageId, title=$title, textScore=${hit.textScore}, vectorScore=${hit.vectorScore}, weighted=$combinedScore" }

            SearchResult.withSimilarity(
                id = actualPageId,
                title = title,
                content = document["content"]?.toString() ?: "",
                path = document["metadata.path"]?.toString() ?: "",
                spaceKey = document["metadata.spaceKey"]?.toString() ?: "",
                keywords = document["metadata.keywords"]?.toString() ?: "",
                similarity = SearchScore.SimilarityScore(combinedScore)
            )
        }.sortedByDescending { it.score }

        log.info { "[${getName()}] Returning ${results.size} results" }
        return results
    }

    private fun buildSearchRequest(
        query: String,
        embedding: List<Float>,
        topK: Int
    ): HybridSearchRequest {
        val fields = getFieldWeights()
        return HybridSearchRequest(
            textQuery = query,
            vectorQuery = embedding,
            fields = com.okestro.okchat.search.client.SearchFields(
                queryBy = fields.queryBy.split(","),
                weights = fields.weights.split(",").map { it.toInt() }
            ),
            filters = mapOf("metadata.type" to "confluence-page"),
            limit = topK
        )
    }

    private fun combineScores(textScore: Double, vectorScore: Double): Double {
        return getWeightSettings().combine(textScore, vectorScore)
    }

    protected abstract fun getFieldWeights(): com.okestro.okchat.search.config.SearchFieldWeightConfig.FieldWeights
    protected abstract fun getWeightSettings(): com.okestro.okchat.search.config.SearchWeightConfig.WeightSettings
}
