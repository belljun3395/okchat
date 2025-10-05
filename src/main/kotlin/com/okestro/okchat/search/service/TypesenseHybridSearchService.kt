package com.okestro.okchat.search.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.okestro.okchat.search.model.SearchScore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
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
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String,
    @Value("\${spring.ai.vectorstore.typesense.client.protocol}") private val protocol: String,
    @Value("\${spring.ai.vectorstore.typesense.client.host}") private val host: String,
    @Value("\${spring.ai.vectorstore.typesense.client.port}") private val port: Int,
    @Value("\${spring.ai.vectorstore.typesense.client.apiKey}") private val apiKey: String
) {
    private val log = KotlinLogging.logger {}

    private val webClient = WebClient.builder()
        .baseUrl("$protocol://$host:$port")
        .defaultHeader("X-TYPESENSE-API-KEY", apiKey)
        .build()

    /**
     * True hybrid search using direct HTTP POST request
     * Combines vector search (semantic) with text search (keyword)
     *
     * @param query Search query
     * @param keywords Extracted keywords for text search (optional, uses query if empty)
     * @param topK Number of results to return
     * @param textWeight Weight for text search (0.0 ~ 1.0)
     * @param vectorWeight Weight for vector search (0.0 ~ 1.0)
     * @return List of search results with combined scores
     */
    suspend fun hybridSearch(
        query: String,
        keywords: String = "",
        topK: Int = 50,
        textWeight: Double = 0.5,
        vectorWeight: Double = 0.5
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Hybrid Search] Query: '$query', Keywords: '$keywords' (topK=$topK)" }

        val searchKeywords = keywords.ifEmpty { query }

        try {
            // Generate embedding for vector search
            log.debug { "[Hybrid Search] Generating query embedding..." }
            val queryEmbedding = embeddingModel.embed(query)
            val embeddingVector = queryEmbedding.joinToString(",")

            // Build search request body
            val searchRequest = TypesenseSearchRequest(
                q = searchKeywords,
                queryBy = "metadata.title,content,metadata.keywords",
                queryByWeights = "5,3,10",
                vectorQuery = "embedding:([$embeddingVector], k:$topK)",
                filterBy = "metadata.type:=confluence-page",
                perPage = topK,
                page = 1
            )

            log.debug { "[Hybrid Search] Executing POST-based hybrid search..." }

            // Execute search via HTTP POST (no URL length limit)
            val response = webClient.post()
                .uri("/collections/$collectionName/documents/search")
                .bodyValue(searchRequest)
                .retrieve()
                .bodyToMono(TypesenseSearchResponse::class.java)
                .awaitSingle()

            val hits = response.hits ?: emptyList()

            log.info { "[Hybrid Search] Found ${hits.size} documents" }

            // Convert to SearchResult with combined scores
            val results = hits.mapNotNull { hit ->
                val document = hit.document ?: return@mapNotNull null
                val metadata = document["metadata"] as? Map<*, *> ?: emptyMap<String, Any>()

                // Text match score (normalize to 0-1 range)
                val textScore = (hit.textMatch?.toDouble() ?: 0.0) / 100.0

                // Vector match score (normalize to 0-1 range)
                val vectorScore = (hit.vectorDistance ?: 1.0).let { distance ->
                    // Convert distance to similarity (lower distance = higher similarity)
                    1.0 / (1.0 + distance)
                }

                // Combine scores with weights
                val combinedScore = (textScore * textWeight) + (vectorScore * vectorWeight)

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
                    similarity = SearchScore.SimilarityScore(combinedScore)
                )
            }.sortedByDescending { it.score }

            log.info { "[Hybrid Search] Returning ${results.size} results with hybrid scoring" }
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

/**
 * Typesense search request DTO
 */
data class TypesenseSearchRequest(
    val q: String,
    @JsonProperty("query_by") val queryBy: String,
    @JsonProperty("query_by_weights") val queryByWeights: String? = null,
    @JsonProperty("vector_query") val vectorQuery: String? = null,
    @JsonProperty("filter_by") val filterBy: String? = null,
    @JsonProperty("per_page") val perPage: Int = 10,
    val page: Int = 1
)

/**
 * Typesense search response DTO
 */
data class TypesenseSearchResponse(
    val hits: List<TypesenseHit>? = null,
    val found: Int? = null
)

data class TypesenseHit(
    val document: Map<String, Any>? = null,
    @JsonProperty("text_match") val textMatch: Long? = null,
    @JsonProperty("vector_distance") val vectorDistance: Double? = null
)
