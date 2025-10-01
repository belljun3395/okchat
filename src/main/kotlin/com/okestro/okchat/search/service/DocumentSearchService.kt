package com.okestro.okchat.search.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

/**
 * Service for searching documents in vector store
 */
@Service
class DocumentSearchService(
    private val vectorStore: VectorStore
) {
    private val log = KotlinLogging.logger {}

    /**
     * Search documents by keyword/query
     *
     * @param query Search keyword or question
     * @param topK Number of results to return (default: 5)
     * @param similarityThreshold Minimum similarity score (0.0 ~ 1.0, default: 0.0)
     * @return List of similar documents
     */
    suspend fun search(
        query: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "Searching for: '$query' (topK=$topK, threshold=$similarityThreshold)" }

        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression("type == 'confluence-page'")
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)

        log.info { "Found ${documents.size} similar documents" }

        documents.map { doc ->
            SearchResult(
                id = doc.metadata["id"]?.toString() ?: "",
                title = doc.metadata["title"]?.toString() ?: "Untitled",
                content = doc.text ?: "",
                path = doc.metadata["path"]?.toString() ?: "",
                spaceKey = doc.metadata["spaceKey"]?.toString() ?: "",
                score = doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 0.0
            )
        }
    }

    /**
     * Search confluence pages only
     */
    suspend fun searchConfluencePages(
        query: String,
        topK: Int = 5
    ): List<SearchResult> {
        return search(query, topK, 0.0)
    }
}

/**
 * Search result data class
 */
data class SearchResult(
    val id: String,
    val title: String,
    val content: String,
    val path: String,
    val spaceKey: String,
    val score: Double
)
