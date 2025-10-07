package com.okestro.okchat.search.model

/**
 * Common interface for all search criteria types.
 *
 * Responsibility:
 * - Define the contract for search criteria
 * - Enable polymorphic handling of different search types
 * - Support Open-Closed Principle: Open for extension, closed for modification
 *
 * Benefits:
 * - New search types can be added without changing existing interfaces
 * - Search strategies can handle criteria generically
 * - Type-safe yet flexible design
 *
 * Example:
 * ```
 * // Adding a new search type doesn't require interface changes
 * data class SearchTags(...) : SearchCriteria {
 *     override fun getSearchType() = SearchType.TAG
 *     override fun toQuery() = tags.joinToString(" OR ")
 * }
 * ```
 */
interface SearchCriteria {

    /**
     * Get the type of this search criteria.
     * Used to determine which field configuration to use.
     */
    fun getSearchType(): SearchType

    /**
     * Convert criteria to a search query string.
     * Different criteria types may combine their terms differently.
     */
    fun toQuery(): String

    /**
     * Check if this criteria has any terms.
     * Used to filter out empty criteria before searching.
     */
    fun isEmpty(): Boolean

    /**
     * Get the number of terms in this criteria.
     * Useful for logging and debugging.
     */
    fun size(): Int
}
