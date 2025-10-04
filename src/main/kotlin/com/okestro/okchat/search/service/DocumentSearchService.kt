package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.model.builder
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
            val distance = doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 1.0
            val keywordMatchCount = countKeywordMatches(keywords, docKeywords)

            //  Type-safe score calculation using builder
            val score = SearchScore.fromDistance(distance)
                .builder()
                .addKeywordBoost(keywordMatchCount, boostPerMatch = 0.1)
                .build()

            SearchResult.withSimilarity(
                id = doc.metadata["id"]?.toString() ?: "",
                title = doc.metadata["title"]?.toString() ?: "Untitled",
                content = doc.text ?: "",
                path = doc.metadata["path"]?.toString() ?: "",
                spaceKey = doc.metadata["spaceKey"]?.toString() ?: "",
                keywords = docKeywords,
                similarity = score
            )
        }.sortedByDescending { it.score } //  Clear: higher similarity is better

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
            val distance = doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 1.0
            //  Type-safe: explicitly convert distance to similarity
            SearchResult.fromVectorSearch(
                id = doc.metadata["id"]?.toString() ?: "",
                title = doc.metadata["title"]?.toString() ?: "Untitled",
                content = doc.text ?: "",
                path = doc.metadata["path"]?.toString() ?: "",
                spaceKey = doc.metadata["spaceKey"]?.toString() ?: "",
                keywords = doc.metadata["keywords"]?.toString() ?: "",
                distance = distance
            )
        }
    }

    /**
     * Title-based search - searches specifically in document titles
     * Uses string matching and semantic similarity on document titles
     *
     * @param query Search query to match against titles
     * @param topK Number of results to return (default: 5)
     * @param similarityThreshold Minimum similarity score (0.0 ~ 1.0, default: 0.0)
     * @return List of documents with matching titles
     */
    suspend fun searchByTitle(
        query: String,
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Title Search] Searching titles for: '$query' (topK=$topK, threshold=$similarityThreshold)" }

        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK * 3) // Get more results for better title matching
            .similarityThreshold(similarityThreshold)
            .filterExpression("type == 'confluence-page'")
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)

        // Calculate title match score and filter/sort
        val results = documents.mapNotNull { doc ->
            val title = doc.metadata["title"]?.toString() ?: return@mapNotNull null
            val titleMatchScore = calculateTitleMatchScore(query, title)

            // Only include if there's some title match
            if (titleMatchScore > 0.0) {
                val distance = doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 1.0
                //  Type-safe score calculation with title boost
                val score = SearchScore.fromDistance(distance)
                    .builder()
                    .addTitleBoost(titleMatchScore)
                    .build()

                SearchResult.withSimilarity(
                    id = doc.metadata["id"]?.toString() ?: "",
                    title = title,
                    content = doc.text ?: "",
                    path = doc.metadata["path"]?.toString() ?: "",
                    spaceKey = doc.metadata["spaceKey"]?.toString() ?: "",
                    keywords = doc.metadata["keywords"]?.toString() ?: "",
                    similarity = score
                )
            } else {
                null
            }
        }
            .sortedByDescending { it.score }
            .take(topK)

        log.info { "[Title Search] Found ${results.size} documents with title matches" }
        results
    }

    /**
     * Calculate title matching score
     * Higher score for exact matches, partial matches, and substring matches
     */
    private fun calculateTitleMatchScore(query: String, title: String): Double {
        val queryLower = query.lowercase()
        val titleLower = title.lowercase()

        var score = 0.0

        // Exact match (highest priority)
        if (titleLower == queryLower) {
            score += 2.0
        }

        // Contains full query
        if (titleLower.contains(queryLower)) {
            score += 1.5
        }

        // Word-by-word matching
        val queryWords = queryLower.split(Regex("\\s+"))
        val titleWords = titleLower.split(Regex("\\s+|_|-"))

        queryWords.forEach { queryWord ->
            if (queryWord.length >= 2) { // Skip very short words
                titleWords.forEach { titleWord ->
                    when {
                        // Exact word match
                        titleWord == queryWord -> score += 0.5
                        // Title word contains query word
                        titleWord.contains(queryWord) -> score += 0.3
                        // Query word contains title word (for short codes)
                        queryWord.contains(titleWord) && titleWord.length >= 2 -> score += 0.2
                    }
                }
            }
        }

        return score
    }

    /**
     * Count keyword matches between search keywords and document keywords
     * Returns the number of matching keywords
     */
    private fun countKeywordMatches(searchKeywords: String, docKeywords: String): Int {
        if (docKeywords.isBlank()) return 0

        val searchTerms = searchKeywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        val docTerms = docKeywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        return searchTerms.count { term ->
            docTerms.any { docTerm ->
                docTerm.contains(term) || term.contains(docTerm)
            }
        }
    }
}

/**
 * Search result data class with type-safe scoring
 */
data class SearchResult(
    val id: String,
    val title: String,
    val content: String,
    val path: String,
    val spaceKey: String,
    val keywords: String = "",
    val score: SearchScore.SimilarityScore //  Type-safe: always similarity (higher is better)
) : Comparable<SearchResult> {
    /**
     * For backward compatibility - returns the similarity value
     */
    val scoreValue: Double get() = score.value

    override fun compareTo(other: SearchResult): Int {
        return score.compareTo(other.score)
    }

    companion object {
        /**
         * Factory method to create SearchResult from vector search with distance
         */
        fun fromVectorSearch(
            id: String,
            title: String,
            content: String,
            path: String,
            spaceKey: String,
            keywords: String = "",
            distance: Double //  Explicitly named "distance"
        ): SearchResult {
            val score = SearchScore.fromDistance(distance).toSimilarity()
            return SearchResult(id, title, content, path, spaceKey, keywords, score)
        }

        /**
         * Factory method to create SearchResult with similarity score
         */
        fun withSimilarity(
            id: String,
            title: String,
            content: String,
            path: String,
            spaceKey: String,
            keywords: String = "",
            similarity: SearchScore.SimilarityScore
        ): SearchResult {
            return SearchResult(id, title, content, path, spaceKey, keywords, similarity)
        }
    }
}
