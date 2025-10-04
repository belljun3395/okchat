package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.SearchScore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.typesense.api.Client
import org.typesense.model.SearchParameters

/**
 * Typesense Hybrid Search Service
 * Combines text-based search with vector search for optimal results
 *
 * This service directly uses Typesense API to leverage:
 * - Text search (BM25-like ranking on title, content, keywords)
 * - Vector search (semantic similarity via embeddings)
 * - Hybrid ranking (combined scores)
 */
@Service
class TypesenseHybridSearchService(
    private val typesenseClient: Client,
    private val embeddingModel: EmbeddingModel,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String
) {
    private val log = KotlinLogging.logger {}

    /**
     * Hybrid search - currently uses text search only
     *
     * NOTE: Vector search via URL query parameters exceeds Typesense's 4000 char limit
     * TODO: Implement POST-based search for true hybrid search in future
     *
     * @param query Search query
     * @param topK Number of results to return
     * @param textWeight Weight for text search (0.0 ~ 1.0)
     * @param vectorWeight Weight for vector search (0.0 ~ 1.0)
     * @return List of search results with combined scores
     */
    suspend fun hybridSearch(
        query: String,
        topK: Int = 50,
        textWeight: Double = 0.5,
        vectorWeight: Double = 0.5
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Hybrid Search] Query: '$query' (topK=$topK)" }
        log.debug { "[Hybrid Search] Currently using text-only search (vector search disabled due to URL length limit)" }

        try {
            // Build search parameters for text search
            // Vector search disabled due to URL length limitation (embedding > 4000 chars)
            val searchParameters = SearchParameters()
                .q(query) // Text query
                .queryBy("metadata.title,content,metadata.keywords") // Fields to search in
                .queryByWeights("5,3,10") // keywords=10, title=5, content=3
                .filterBy("metadata.type:=confluence-page") // Filter by type
                .perPage(topK) // Limit results
                .page(1)
                .sortBy("_text_match:desc") // Sort by text match score

            log.debug { "[Hybrid Search] Executing Typesense text search..." }

            // Execute search
            val searchResult = typesenseClient.collections(collectionName)
                .documents()
                .search(searchParameters)

            val hits = searchResult.hits ?: emptyList()

            log.info { "[Hybrid Search] Found ${hits.size} documents" }

            // Convert to SearchResult
            val results = hits.mapNotNull { hit ->
                val document = hit.document ?: return@mapNotNull null
                val metadata = document["metadata"] as? Map<*, *> ?: emptyMap<String, Any>()

                // Text match score (normalize to 0-1 range)
                val textScore = (hit.textMatch?.toDouble() ?: 0.0) / 100.0

                // Extract actual page ID (remove chunk suffix if present)
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
                    similarity = SearchScore.SimilarityScore(textScore)
                )
            }

            log.info { "[Hybrid Search] Returning ${results.size} results" }
            results
        } catch (e: Exception) {
            log.error(e) { "[Hybrid Search] Error during search: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Text-only search using Typesense
     * Best for exact keyword matching
     */
    suspend fun textSearch(
        query: String,
        topK: Int = 50,
        queryBy: String = "metadata.title,metadata.keywords,content"
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Text Search] Query: '$query' (topK=$topK)" }

        try {
            val searchParameters = SearchParameters()
                .q(query)
                .queryBy(queryBy)
                .filterBy("metadata.type:=confluence-page")
                .perPage(topK)
                .page(1)

            val searchResult = typesenseClient.collections(collectionName)
                .documents()
                .search(searchParameters)

            val hits = searchResult.hits ?: emptyList()

            log.info { "[Text Search] Found ${hits.size} documents" }

            hits.mapNotNull { hit ->
                val document = hit.document ?: return@mapNotNull null
                val metadata = document["metadata"] as? Map<*, *> ?: emptyMap<String, Any>()
                // Normalize Typesense text match score
                val score = (hit.textMatch?.toDouble() ?: 0.0) / 100.0

                // Extract actual page ID (remove chunk suffix if present)
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
                    similarity = SearchScore.SimilarityScore(score)
                )
            }
        } catch (e: Exception) {
            log.error(e) { "[Text Search] Error during search: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Keyword-focused search
     * Prioritizes matching in keywords and title fields
     */
    suspend fun keywordSearch(
        keywords: String,
        topK: Int = 50
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Keyword Text Search] Keywords: '$keywords' (topK=$topK)" }

        try {
            // Boost keywords field heavily, then title, then content
            val searchParameters = SearchParameters()
                .q(keywords)
                .queryBy("metadata.keywords,metadata.title,content")
                .queryByWeights("10,5,1") // Heavy weight on keywords
                .filterBy("metadata.type:=confluence-page")
                .perPage(topK)
                .page(1)
                .sortBy("_text_match:desc") // Sort by text match score

            val searchResult = typesenseClient.collections(collectionName)
                .documents()
                .search(searchParameters)

            val hits = searchResult.hits ?: emptyList()

            log.info { "[Keyword Text Search] Found ${hits.size} documents" }

            hits.mapNotNull { hit ->
                val document = hit.document ?: return@mapNotNull null
                val metadata = document["metadata"] as? Map<*, *> ?: emptyMap<String, Any>()
                // Normalize Typesense text match score
                val score = (hit.textMatch?.toDouble() ?: 0.0) / 100.0

                // Extract actual page ID (remove chunk suffix if present)
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
                    similarity = SearchScore.SimilarityScore(score)
                )
            }
        } catch (e: Exception) {
            log.error(e) { "[Keyword Text Search] Error during search: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Title-focused search
     * Prioritizes matching in title field
     */
    suspend fun titleSearch(
        query: String,
        topK: Int = 50
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Title Text Search] Query: '$query' (topK=$topK)" }

        try {
            val searchParameters = SearchParameters()
                .q(query)
                .queryBy("metadata.title,content")
                .queryByWeights("10,1") // Heavy weight on title
                .filterBy("metadata.type:=confluence-page")
                .perPage(topK)
                .page(1)
                .sortBy("_text_match:desc")

            val searchResult = typesenseClient.collections(collectionName)
                .documents()
                .search(searchParameters)

            val hits = searchResult.hits ?: emptyList()

            log.info { "[Title Text Search] Found ${hits.size} documents" }

            hits.mapNotNull { hit ->
                val document = hit.document ?: return@mapNotNull null
                val metadata = document["metadata"] as? Map<*, *> ?: emptyMap<String, Any>()
                // Normalize Typesense text match score
                val score = (hit.textMatch?.toDouble() ?: 0.0) / 100.0

                // Extract actual page ID (remove chunk suffix if present)
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
                    similarity = SearchScore.SimilarityScore(score)
                )
            }
        } catch (e: Exception) {
            log.error(e) { "[Title Text Search] Error during search: ${e.message}" }
            emptyList()
        }
    }
}
