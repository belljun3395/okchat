package com.okestro.okchat.search.model

/**
 * Type-safe wrappers for different types of search results.
 * Each wrapper provides type safety and can have specialized methods.
 */

/**
 * Results from keyword-based search
 */
data class KeywordSearchResults(
    val results: List<SearchResult>
) {
    val size: Int get() = results.size
    val isEmpty: Boolean get() = results.isEmpty()
    val isNotEmpty: Boolean get() = results.isNotEmpty()

    fun topN(n: Int): List<SearchResult> = results.take(n)

    companion object {
        fun empty() = KeywordSearchResults(emptyList())
    }
}

/**
 * Results from title-based search
 */
data class TitleSearchResults(
    val results: List<SearchResult>
) {
    val size: Int get() = results.size
    val isEmpty: Boolean get() = results.isEmpty()
    val isNotEmpty: Boolean get() = results.isNotEmpty()

    fun topN(n: Int): List<SearchResult> = results.take(n)

    companion object {
        fun empty() = TitleSearchResults(emptyList())
    }
}

/**
 * Results from content-based search
 */
data class ContentSearchResults(
    val results: List<SearchResult>
) {
    val size: Int get() = results.size
    val isEmpty: Boolean get() = results.isEmpty()
    val isNotEmpty: Boolean get() = results.isNotEmpty()

    fun topN(n: Int): List<SearchResult> = results.take(n)

    companion object {
        fun empty() = ContentSearchResults(emptyList())
    }
}

/**
 * Results from path-based search
 */
data class PathSearchResults(
    val results: List<SearchResult>
) {
    val size: Int get() = results.size
    val isEmpty: Boolean get() = results.isEmpty()
    val isNotEmpty: Boolean get() = results.isNotEmpty()

    fun topN(n: Int): List<SearchResult> = results.take(n)

    companion object {
        fun empty() = PathSearchResults(emptyList())
    }
}
