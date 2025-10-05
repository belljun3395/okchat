package com.okestro.okchat.search.model

/**
 * Result container for multi-search operations
 * Holds results from keyword, title, and content searches
 */
data class MultiSearchResult(
    val keywordResults: List<SearchResult>,
    val titleResults: List<SearchResult>,
    val contentResults: List<SearchResult>
) {
    /**
     * Get all results combined (useful for displaying all search results)
     */
    fun getAllResults(): List<SearchResult> {
        return (keywordResults + titleResults + contentResults).distinctBy { it.id }
    }

    /**
     * Get total number of unique documents found across all search types
     */
    fun getTotalUniqueDocuments(): Int {
        return (keywordResults + titleResults + contentResults)
            .map { it.id }
            .distinct()
            .size
    }

    /**
     * Check if any search returned results
     */
    fun hasResults(): Boolean {
        return keywordResults.isNotEmpty() || titleResults.isNotEmpty() || contentResults.isNotEmpty()
    }
}
