package com.okestro.okchat.search.util

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Utility functions for hybrid search operations
 * Provides common logic for building requests, parsing responses, and deduplicating results
 */
object HybridSearchUtils {

    /**
     * Build search request with field configuration
     */
    fun buildSearchRequest(
        query: String,
        embedding: List<Float>,
        fields: SearchFieldWeightConfig.FieldWeights,
        topK: Int
    ): HybridSearchRequest {
        return HybridSearchRequest(
            textQuery = query,
            vectorQuery = embedding,
            fields = SearchFields(
                queryBy = fields.queryBy.split(","),
                weights = fields.weights.split(",").map { it.toInt() }
            ),
            filters = mapOf("metadata.type" to "confluence-page"),
            limit = topK
        )
    }

    /**
     * Parse search response hits into SearchResult objects
     */
    fun parseSearchResults(
        response: HybridSearchResponse,
        scoresCombiner: (Double, Double) -> Double = { text, vector -> text + vector }
    ): List<SearchResult> {
        return response.hits.map { hit ->
            val document = hit.document

            val rawId = document["id"]?.toString() ?: ""
            val actualPageId = if (rawId.contains("_chunk_")) {
                rawId.substringBefore("_chunk_")
            } else {
                rawId
            }

            // Extract metadata - support both flat (metadata.title) and nested (metadata.title) structure
            @Suppress("UNCHECKED_CAST")
            val metadata = document["metadata"] as? Map<String, Any> ?: emptyMap()

            // Try flat structure first (metadata.title), fallback to nested (metadata.title)
            val title = document["metadata.title"]?.toString() ?: metadata["title"]?.toString() ?: "Untitled"
            val path = document["metadata.path"]?.toString() ?: metadata["path"]?.toString() ?: ""
            val spaceKey = document["metadata.spaceKey"]?.toString() ?: metadata["spaceKey"]?.toString() ?: ""
            val keywords = document["metadata.keywords"]?.toString() ?: metadata["keywords"]?.toString() ?: ""

            val combinedScore = scoresCombiner(hit.textScore, hit.vectorScore)

            log.trace { "[Parse] id=$actualPageId, title=$title, textScore=${hit.textScore}, vectorScore=${hit.vectorScore}, combined=$combinedScore" }

            SearchResult.withSimilarity(
                id = actualPageId,
                title = title,
                content = document["content"]?.toString() ?: "",
                path = path,
                spaceKey = spaceKey,
                keywords = keywords,
                similarity = SearchScore.SimilarityScore(combinedScore)
            )
        }.sortedByDescending { it.score.value }
    }

    /**
     * Deduplicate search results by page ID, merging chunks from the same page
     */
    fun deduplicateResults(results: List<SearchResult>): List<SearchResult> {
        return results
            .groupBy { it.id }
            .mapValues { (pageId, pageResults) ->
                if (pageResults.size == 1) {
                    pageResults.first()
                } else {
                    log.debug { "[Merge] Page $pageId (${pageResults.first().title}): Merging ${pageResults.size} chunks" }

                    val baseResult = pageResults.maxByOrNull { it.score.value }!!

                    val sortedChunks = pageResults.sortedBy { result ->
                        result.id
                    }

                    val mergedContent = sortedChunks
                        .joinToString("\n\n") { it.content }
                        .trim()

                    val chunkSizes = pageResults.joinToString("+") { "${it.content.length}" }
                    log.debug { "[Merge] Page $pageId: Merged ${mergedContent.length} chars from ${pageResults.size} chunks [$chunkSizes]" }

                    baseResult.copy(content = mergedContent)
                }
            }
            .values
            .sortedByDescending { it.score.value }
    }
}
