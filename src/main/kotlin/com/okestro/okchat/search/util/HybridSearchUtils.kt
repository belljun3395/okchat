package com.okestro.okchat.search.util

import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.model.SearchDocument
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
     * Parse search response hits into SearchResult objects
     */
    fun parseSearchResults(
        response: HybridSearchResponse,
        scoresCombiner: (Double, Double) -> Double = { text, vector -> text + vector }
    ): List<SearchResult> {
        return response.hits.map { hit ->
            // Convert untyped map to type-safe SearchDocument
            val document = SearchDocument.fromMap(hit.document)

            val actualPageId = document.getActualPageId()
            val title = document.getTitle()
            val path = document.getPath()
            val spaceKey = document.getSpaceKey()
            val keywords = document.getKeywords()
            val type = document.getType() // Get document type (page or PDF attachment)
            val knowledgeBaseId = document.getKnowledgeBaseId()

            // Extract link information from metadata
            val webUrl = document.metadata.getStringValue("webUrl")
            val downloadUrl = document.metadata.getStringValue("downloadUrl")

            val combinedScore = scoresCombiner(hit.textScore, hit.vectorScore)

            log.trace { "[Parse] id=$actualPageId, title=$title, type=$type, kb=$knowledgeBaseId, textScore=${hit.textScore}, vectorScore=${hit.vectorScore}, combined=$combinedScore" }

            SearchResult.withSimilarity(
                id = actualPageId,
                title = title,
                content = document.content,
                path = path,
                spaceKey = spaceKey,
                knowledgeBaseId = knowledgeBaseId,
                keywords = keywords,
                similarity = SearchScore.SimilarityScore(combinedScore),
                type = type,
                webUrl = webUrl,
                downloadUrl = downloadUrl
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
