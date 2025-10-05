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
 * Enhanced with Typesense hybrid search for better accuracy
 */
@Service
class DocumentSearchService(
    private val vectorStore: VectorStore,
    private val typesenseHybridSearchService: TypesenseHybridSearchService
) {
    private val log = KotlinLogging.logger {}

    // Flag to enable/disable hybrid search (can be made configurable)
    private val useHybridSearch = true

    /**
     * Keyword-based search - searches specifically in document keywords metadata
     * ENHANCED: Uses Typesense text search for exact keyword matching
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

        // Use hybrid search if enabled, otherwise fall back to vector search
        if (useHybridSearch) {
            val results = typesenseHybridSearchService.keywordSearch(keywords, topK)
            log.info { "[Keyword Search] Found ${results.size} documents (Typesense text search)" }
            return@withContext results.filter { it.score.value >= similarityThreshold }
        }

        // Fallback: Vector search with keyword boost
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

        log.info { "[Keyword Search] Found ${results.size} documents with keyword matches (vector fallback)" }
        results
    }

    /**
     * Content-based search - searches specifically in document content
     * ENHANCED: Uses Typesense hybrid search (text + vector) for best results
     *
     * @param query Search query related to content
     * @param keywords Extracted keywords for text search (optional)
     * @param topK Number of results to return (default: 5)
     * @param similarityThreshold Minimum similarity score (0.0 ~ 1.0, default: 0.0)
     * @return List of documents with similar content
     */
    suspend fun searchByContent(
        query: String,
        keywords: String = "",
        topK: Int = 5,
        similarityThreshold: Double = 0.0
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        log.info { "[Content Search] Searching content for: '$query', keywords: '$keywords' (topK=$topK, threshold=$similarityThreshold)" }

        // Use hybrid search if enabled (best for content search)
        if (useHybridSearch) {
            val results = typesenseHybridSearchService.hybridSearch(
                query = query,
                keywords = keywords,
                topK = topK,
                textWeight = 0.4, // Balance between text and semantic
                vectorWeight = 0.6 // Slightly prefer semantic for content
            )
            log.info { "[Content Search] Found ${results.size} documents (Typesense hybrid search)" }
            return@withContext results.filter { it.score.value >= similarityThreshold }
        }

        // Fallback: Pure vector search
        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression("type == 'confluence-page'")
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)

        log.info { "[Content Search] Found ${documents.size} documents with similar content (vector fallback)" }

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
     * ENHANCED: Uses Typesense text search with heavy title weighting
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

        // Use hybrid search if enabled
        if (useHybridSearch) {
            val results = typesenseHybridSearchService.titleSearch(query, topK)
            log.info { "[Title Search] Found ${results.size} documents (Typesense text search)" }
            return@withContext results.filter { it.score.value >= similarityThreshold }
        }

        // Fallback: Vector search with title matching boost
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

        log.info { "[Title Search] Found ${results.size} documents with title matches (vector fallback)" }
        results
    }

    /**
     * Calculate title matching score
     * Higher score for exact matches, partial matches, and substring matches
     * RRF-optimized: Strong boost for date pattern matches (YYMMDD format)
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

        // Date pattern matching (e.g., "2509" matches "250908_주간회의")
        // Extract date-like patterns from query (4-6 digit numbers)
        val datePatterns = Regex("\\d{4,6}").findAll(queryLower)
        datePatterns.forEach { pattern ->
            val dateStr = pattern.value
            // Strong match if title starts with this date pattern
            if (titleLower.startsWith(dateStr)) {
                score += 3.0 // Higher than exact match for date-prefixed titles
            } else if (titleLower.contains(dateStr)) {
                score += 2.0 // Contains date pattern
            }
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
                        // Title starts with query word (prefix match for dates)
                        titleWord.startsWith(queryWord) && queryWord.length >= 4 -> score += 0.8
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
