package com.okestro.okchat.search.model

/**
 * Sealed interface for all search result types.
 * Provides type safety while enabling polymorphic handling.
 *
 * Responsibility:
 * - Define common operations for all search result types
 * - Enable type-safe polymorphism
 * - Support Open-Closed Principle for result types
 */
sealed interface TypedSearchResults {
    val results: List<SearchResult>
    val type: SearchType

    val size: Int get() = results.size
    val isEmpty: Boolean get() = results.isEmpty()
    val isNotEmpty: Boolean get() = results.isNotEmpty()

    fun topN(n: Int): List<SearchResult> = results.take(n)

    companion object {
        /**
         * Factory method to create results by type
         */
        fun of(type: SearchType, results: List<SearchResult>): TypedSearchResults {
            return when (type) {
                SearchType.KEYWORD -> KeywordSearchResults(results)
                SearchType.TITLE -> TitleSearchResults(results)
                SearchType.CONTENT -> ContentSearchResults(results)
                SearchType.PATH -> PathSearchResults(results)
            }
        }
    }
}

/**
 * Results from keyword-based search
 */
data class KeywordSearchResults(
    override val results: List<SearchResult>
) : TypedSearchResults {
    override val type: SearchType = SearchType.KEYWORD

    companion object {
        fun empty() = KeywordSearchResults(emptyList())
    }
}

/**
 * Results from title-based search
 */
data class TitleSearchResults(
    override val results: List<SearchResult>
) : TypedSearchResults {
    override val type: SearchType = SearchType.TITLE

    companion object {
        fun empty() = TitleSearchResults(emptyList())
    }
}

/**
 * Results from content-based search
 */
data class ContentSearchResults(
    override val results: List<SearchResult>
) : TypedSearchResults {
    override val type: SearchType = SearchType.CONTENT

    companion object {
        fun empty() = ContentSearchResults(emptyList())
    }
}

/**
 * Results from path-based search
 */
data class PathSearchResults(
    override val results: List<SearchResult>
) : TypedSearchResults {
    override val type: SearchType = SearchType.PATH

    companion object {
        fun empty() = PathSearchResults(emptyList())
    }
}
