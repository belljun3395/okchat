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
    val score: SearchScore.SimilarityScore
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
            distance: Double
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
