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
     * Keyword-based search - searches specifically in document keywords metadata
     * This is useful when you want to search using extracted keywords
     *
     * @param keywords Keywords to search for
     * @param topK Number of results to return (default: 5)
     * @param similarityThreshold Minimum similarity score (0.0 ~ 1.0, default: 0.0)
     * @return List of documents matching the keywords
     */
    suspend fun searchByKeywords(
        keywords: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Keyword Search] Searching with keywords: '$keywords' (topK=$topK, threshold=$similarityThreshold)" }

        // Search using keywords field in semantic search
        // The query will be matched against document embeddings, but we can boost keyword matches
        val searchRequest = SearchRequest.builder()
            .query(keywords)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression("type == 'confluence-page'")
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)

        // Filter and sort by keyword relevance
        val results = documents.map { doc ->
            val docKeywords = doc.metadata["keywords"]?.toString() ?: ""
            val keywordScore = calculateKeywordScore(keywords, docKeywords)

            SearchResult(
                id = doc.metadata["id"]?.toString() ?: "",
                title = doc.metadata["title"]?.toString() ?: "Untitled",
                content = doc.text ?: "",
                path = doc.metadata["path"]?.toString() ?: "",
                spaceKey = doc.metadata["spaceKey"]?.toString() ?: "",
                keywords = docKeywords,
                score = (doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 0.0) + keywordScore
            )
        }.sortedByDescending { it.score }

        log.info { "[Keyword Search] Found ${results.size} documents with keyword matches" }
        results
    }

    /**
     * Content-based search - searches specifically in document content
     * Uses semantic similarity on document content
     *
     * @param query Search query related to content
     * @param topK Number of results to return (default: 5)
     * @param similarityThreshold Minimum similarity score (0.0 ~ 1.0, default: 0.0)
     * @return List of documents with similar content
     */
    suspend fun searchByContent(
        query: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Content Search] Searching content for: '$query' (topK=$topK, threshold=$similarityThreshold)" }

        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression("type == 'confluence-page'")
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)

        log.info { "[Content Search] Found ${documents.size} documents with similar content" }

        documents.map { doc ->
            SearchResult(
                id = doc.metadata["id"]?.toString() ?: "",
                title = doc.metadata["title"]?.toString() ?: "Untitled",
                content = doc.text ?: "",
                path = doc.metadata["path"]?.toString() ?: "",
                spaceKey = doc.metadata["spaceKey"]?.toString() ?: "",
                keywords = doc.metadata["keywords"]?.toString() ?: "",
                score = doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 0.0
            )
        }
    }

    /**
     * Calculate keyword matching score
     * Higher score if more keywords match
     */
    private fun calculateKeywordScore(searchKeywords: String, docKeywords: String): Double {
        if (docKeywords.isBlank()) return 0.0

        val searchTerms = searchKeywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        val docTerms = docKeywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val matchCount = searchTerms.count { term ->
            docTerms.any { docTerm ->
                docTerm.contains(term) || term.contains(docTerm)
            }
        }

        // Boost score by 0.1 for each matching keyword
        return matchCount * 0.1
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
    val keywords: String = "",
    val score: Double
)
