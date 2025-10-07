package com.okestro.okchat.search.model

/**
 * Result container for multi-search operations.
 * Uses Map-based approach for Open-Closed Principle.
 *
 * Responsibility:
 * - Hold results from multiple search types
 * - Provide convenient access methods
 * - Support extensibility (new search types don't require class changes)
 *
 * Benefits:
 * - Extensible: Add new search types without modifying this class
 * - Type-safe: Uses TypedSearchResults sealed interface
 * - Flexible: Results accessed by SearchType enum
 */
data class MultiSearchResult(
    private val resultsByType: Map<SearchType, TypedSearchResults>
) {

    /**
     * Convenience properties for backward compatibility and common usage
     */
    val keywordResults: KeywordSearchResults
        get() = resultsByType[SearchType.KEYWORD] as? KeywordSearchResults ?: KeywordSearchResults.empty()

    val titleResults: TitleSearchResults
        get() = resultsByType[SearchType.TITLE] as? TitleSearchResults ?: TitleSearchResults.empty()

    val contentResults: ContentSearchResults
        get() = resultsByType[SearchType.CONTENT] as? ContentSearchResults ?: ContentSearchResults.empty()

    val pathResults: PathSearchResults
        get() = resultsByType[SearchType.PATH] as? PathSearchResults ?: PathSearchResults.empty()

    companion object {
        /**
         * Create an empty multi-search result
         */
        fun empty() = MultiSearchResult(emptyMap())

        /**
         * Builder for creating MultiSearchResult from Map
         */
        fun fromMap(results: Map<SearchType, TypedSearchResults>) = MultiSearchResult(results)
    }
}
