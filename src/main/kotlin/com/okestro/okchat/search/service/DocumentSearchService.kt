package com.okestro.okchat.search.service

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Simplified document search service using Strategy Pattern
 * Delegates search logic to specialized strategies
 */
@Service
class DocumentSearchService(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel,
    private val fieldConfig: SearchFieldWeightConfig
) {

    /**
     * Execute multiple searches in a single request (optimized with Typesense multi_search)
     * Returns results for [keyword, title, content] searches in that order
     *
     * This dramatically reduces network latency by batching all searches into one HTTP request
     */
    suspend fun multiSearch(
        query: String,
        keywords: String,
        topK: Int = 50
    ): Triple<List<SearchResult>, List<SearchResult>, List<SearchResult>> {
        log.info { "[Multi-Search] Executing optimized multi-search: query='$query', keywords='$keywords', topK=$topK" }

        // Generate embedding once (reused for all searches)
        log.debug { "[Multi-Search] Generating embedding..." }
        val embedding = embeddingModel.embed(query).toList()

        // Build 3 search requests with different field configurations
        val requests = listOf(
            // 1. Keyword search
            buildSearchRequest(
                textQuery = keywords,
                embedding = embedding,
                fields = fieldConfig.keyword,
                topK = topK
            ),
            // 2. Title search
            buildSearchRequest(
                textQuery = query,
                embedding = embedding,
                fields = fieldConfig.title,
                topK = topK
            ),
            // 3. Content search
            buildSearchRequest(
                textQuery = keywords.ifEmpty { query },
                embedding = embedding,
                fields = fieldConfig.content,
                topK = topK
            )
        )

        // Execute all searches in a single HTTP request
        log.debug { "[Multi-Search] Executing batched search..." }
        val responses = searchClient.multiHybridSearch(requests)

        // Parse responses into SearchResults and deduplicate
        log.debug { "[Multi-Search] Parsing keyword results..." }
        val keywordResults = deduplicateResults(parseSearchResults(responses[0]))

        log.debug { "[Multi-Search] Parsing title results..." }
        val titleResults = deduplicateResults(parseSearchResults(responses[1]))

        log.debug { "[Multi-Search] Parsing content results..." }
        val contentResults = deduplicateResults(parseSearchResults(responses[2]))

        log.info {
            "[Multi-Search] Completed: keyword=${keywordResults.size}, " +
                "title=${titleResults.size}, content=${contentResults.size}"
        }

        // Log top results from each search type (DEBUG only)
        if (log.isDebugEnabled()) {
            log.debug { "[Multi-Search] ━━━ Keyword search top 5 ━━━" }
            keywordResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [K${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }

            log.debug { "[Multi-Search] ━━━ Title search top 5 ━━━" }
            titleResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [T${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }

            log.debug { "[Multi-Search] ━━━ Content search top 5 ━━━" }
            contentResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [C${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }
        }

        return Triple(keywordResults, titleResults, contentResults)
    }

    private fun buildSearchRequest(
        textQuery: String,
        embedding: List<Float>,
        fields: SearchFieldWeightConfig.FieldWeights,
        topK: Int
    ): HybridSearchRequest {
        return HybridSearchRequest(
            textQuery = textQuery,
            vectorQuery = embedding,
            fields = SearchFields(
                queryBy = fields.queryBy.split(","),
                weights = fields.weights.split(",").map { it.toInt() }
            ),
            filters = mapOf("metadata.type" to "confluence-page"),
            limit = topK
        )
    }

    private fun parseSearchResults(response: com.okestro.okchat.search.client.HybridSearchResponse): List<SearchResult> {
        return response.hits.map { hit ->
            val document = hit.document

            // Typesense stores metadata as flat keys with dot notation (metadata.title, metadata.keywords, etc.)
            // NOT as nested objects
            val rawId = document["id"]?.toString() ?: ""
            val actualPageId = if (rawId.contains("_chunk_")) {
                rawId.substringBefore("_chunk_")
            } else {
                rawId
            }

            // Scores are already normalized (0-1 range) by TypesenseSearchClientAdapter
            val combinedScore = hit.textScore + hit.vectorScore

            val title = document["metadata.title"]?.toString() ?: "Untitled"
            val content = document["content"]?.toString() ?: ""
            val path = document["metadata.path"]?.toString() ?: ""
            val spaceKey = document["metadata.spaceKey"]?.toString() ?: ""
            val keywords = document["metadata.keywords"]?.toString() ?: ""

            log.trace { "[Parse] id=$actualPageId, title=$title, textScore=${hit.textScore}, vectorScore=${hit.vectorScore}, combined=$combinedScore" }

            SearchResult.withSimilarity(
                id = actualPageId,
                title = title,
                content = content,
                path = path,
                spaceKey = spaceKey,
                keywords = keywords,
                similarity = SearchScore.SimilarityScore(combinedScore)
            )
        }
    }

    /**
     * Deduplicate search results by page ID, keeping the best chunk for each page
     * This is important when searching chunked documents where multiple chunks from the same page may match
     * * Strategy: Prefer chunks with actual content over metadata-only chunks
     */
    private fun deduplicateResults(results: List<SearchResult>): List<SearchResult> {
        return results
            .groupBy { it.id }
            .mapValues { (pageId, pageResults) ->
                if (pageResults.size == 1) {
                    // Single result, no merging needed
                    pageResults.first()
                } else {
                    // Multiple chunks from same page - merge all content
                    log.debug { "[Merge] Page $pageId (${pageResults.first().title}): Merging ${pageResults.size} chunks" }

                    // Use the highest scoring chunk as base
                    val baseResult = pageResults.maxByOrNull { it.score.value }!!

                    // Sort chunks by chunk index for correct order
                    val sortedChunks = pageResults.sortedBy { result ->
                        val id = result.id
                        // Extract chunk index from ID like "123_chunk_2"
                        if (id.contains("_chunk_")) {
                            id.substringAfterLast("_chunk_").toIntOrNull() ?: 0
                        } else {
                            0
                        }
                    }

                    // Merge all content from all chunks in correct order
                    val mergedContent = sortedChunks
                        .joinToString("\n\n") { it.content }
                        .trim()

                    val chunkSizes = pageResults.joinToString("+") { "${it.content.length}" }
                    log.debug { "[Merge] Page $pageId: Merged ${mergedContent.length} chars from ${pageResults.size} chunks [$chunkSizes]" }

                    // Return merged result with combined content
                    baseResult.copy(content = mergedContent)
                }
            }
            .values
            .sortedByDescending { it.score.value }
    }
}
