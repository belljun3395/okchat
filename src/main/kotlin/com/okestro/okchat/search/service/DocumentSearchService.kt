package com.okestro.okchat.search.service

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.config.SearchWeightConfig
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.strategy.ContentSearchStrategy
import com.okestro.okchat.search.strategy.KeywordSearchStrategy
import com.okestro.okchat.search.strategy.TitleSearchStrategy
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
    private val keywordStrategy: KeywordSearchStrategy,
    private val titleStrategy: TitleSearchStrategy,
    private val contentStrategy: ContentSearchStrategy,
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel,
    private val weightConfig: SearchWeightConfig,
    private val fieldConfig: SearchFieldWeightConfig
) {

    /**
     * Search by keywords with hybrid search (text + vector)
     */
    suspend fun searchByKeywords(
        keywords: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> {
        log.info { "[Keyword Search] keywords='$keywords', topK=$topK" }

        return keywordStrategy.search(keywords, topK)
            .filter { it.score.value >= similarityThreshold }
    }

    /**
     * Search by title with hybrid search (text + vector)
     */
    suspend fun searchByTitle(
        query: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> {
        log.info { "[Title Search] query='$query', topK=$topK" }

        return titleStrategy.search(query, topK)
            .filter { it.score.value >= similarityThreshold }
    }

    /**
     * Search by content with hybrid search (text + vector)
     */
    suspend fun searchByContent(
        query: String,
        keywords: String = "",
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> {
        log.info { "[Content Search] query='$query', keywords='$keywords', topK=$topK" }

        // Use keywords if provided, otherwise use query
        val searchQuery = keywords.ifEmpty { query }

        return contentStrategy.search(searchQuery, topK)
            .filter { it.score.value >= similarityThreshold }
    }

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

        // Parse responses into SearchResults
        val keywordResults = parseSearchResults(responses[0])
        val titleResults = parseSearchResults(responses[1])
        val contentResults = parseSearchResults(responses[2])

        log.info {
            "[Multi-Search] Completed: keyword=${keywordResults.size}, " +
                "title=${titleResults.size}, content=${contentResults.size}"
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
            val metadata = document["metadata"] as? Map<*, *> ?: emptyMap<String, Any>()

            // Combine text and vector scores based on weight settings
            val combinedScore = hit.textScore + hit.vectorScore // Already weighted by Typesense

            // Handle chunked documents - extract actual page ID
            val rawId = metadata["id"]?.toString() ?: ""
            val actualPageId = if (rawId.contains("_chunk_")) {
                rawId.substringBefore("_chunk_")
            } else {
                rawId
            }

            SearchResult.withSimilarity(
                id = actualPageId,
                title = metadata["title"]?.toString() ?: "Untitled",
                content = document["content"]?.toString() ?: "",
                path = metadata["path"]?.toString() ?: "",
                spaceKey = metadata["spaceKey"]?.toString() ?: "",
                keywords = metadata["keywords"]?.toString() ?: "",
                similarity = SearchScore.SimilarityScore(combinedScore)
            )
        }
    }
}
