package com.okestro.okchat.search.model

/**
 * Search result data class with type-safe scoring
 */
data class SearchResult(
    val id: String,
    val title: String,
    var content: String,
    val path: String,
    val spaceKey: String,
    val keywords: String = "",
    val score: SearchScore.SimilarityScore,
    val type: String = "confluence-page", // Document type: confluence-page or confluence-pdf-attachment
    val pageId: String = "" // For PDF attachments, this is the parent page ID
) : Comparable<SearchResult> {
    /**
     * For backward compatibility - returns the similarity value
     */
    val scoreValue: Double get() = score.value

    override fun compareTo(other: SearchResult): Int {
        return score.compareTo(other.score)
    }

    fun combineContent(other: String) {
        this.content = this.content + "\n\n" + other
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
            distance: Double,
            type: String = "confluence-page",
            pageId: String = ""
        ): SearchResult {
            val score = SearchScore.fromDistance(distance).toSimilarity()
            return SearchResult(id, title, content, path, spaceKey, keywords, score, type, pageId)
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
            similarity: SearchScore.SimilarityScore,
            type: String = "confluence-page",
            pageId: String = ""
        ): SearchResult {
            return SearchResult(id, title, content, path, spaceKey, keywords, similarity, type, pageId)
        }
    }
}
