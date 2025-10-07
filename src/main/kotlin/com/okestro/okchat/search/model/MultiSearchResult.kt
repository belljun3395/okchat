package com.okestro.okchat.search.model

/**
 * Result container for multi-search operations.
 * Holds type-safe results from keyword, title, content, and path searches.
 *
 * Each result type is wrapped in its own class for:
 * - Type safety (prevents mixing up result types)
 * - Semantic clarity (explicit meaning of each result set)
 * - Extensibility (each type can have specialized methods)
 */
data class MultiSearchResult(
    val keywordResults: KeywordSearchResults,
    val titleResults: TitleSearchResults,
    val contentResults: ContentSearchResults,
    val pathResults: PathSearchResults
) {
    /**
     * Get all results combined (useful for displaying all search results)
     */
    fun getAllResults(): List<SearchResult> {
        return (
            keywordResults.results +
                titleResults.results +
                contentResults.results +
                pathResults.results
            ).distinctBy { it.id }
    }

    /**
     * Get total number of unique documents found across all search types
     */
    fun getTotalUniqueDocuments(): Int {
        return getAllResults().size
    }

    /**
     * Check if any search returned results
     */
    fun hasResults(): Boolean {
        return keywordResults.isNotEmpty ||
            titleResults.isNotEmpty ||
            contentResults.isNotEmpty ||
            pathResults.isNotEmpty
    }

    /**
     * Get summary statistics of search results
     */
    fun getSummary(): SearchSummary {
        return SearchSummary(
            keywordCount = keywordResults.size,
            titleCount = titleResults.size,
            contentCount = contentResults.size,
            pathCount = pathResults.size,
            totalUnique = getTotalUniqueDocuments()
        )
    }

    data class SearchSummary(
        val keywordCount: Int,
        val titleCount: Int,
        val contentCount: Int,
        val pathCount: Int,
        val totalUnique: Int
    )
}
