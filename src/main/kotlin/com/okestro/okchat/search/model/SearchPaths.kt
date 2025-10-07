package com.okestro.okchat.search.model

/**
 * Value object representing search paths.
 * Implements SearchCriteria for polymorphic handling.
 */
data class SearchPaths(
    val paths: List<SearchPath>
) : SearchCriteria {

    override fun getSearchType(): SearchType = SearchType.PATH

    override fun toQuery(): String = toOrQuery()
    companion object {
        fun fromStrings(terms: List<String>): SearchPaths {
            return SearchPaths(
                terms.filter { it.isNotBlank() }
                    .mapIndexed { index, term ->
                        SearchPath(term = term, weight = (index + 1) / terms.size.toDouble())
                    }
            )
        }
    }

    /**
     * Convert to OR query (takes top 5 keywords to avoid noise)
     */
    fun toOrQuery(): String {
        return paths
            .filter { it.isValid() }
            .joinToString(" OR ") { it.term }
    }

    override fun isEmpty(): Boolean = paths.isEmpty()

    override fun size(): Int = paths.size

    fun terms(): List<String> = paths.map { it.term }
}
