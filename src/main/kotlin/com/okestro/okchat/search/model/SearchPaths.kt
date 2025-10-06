package com.okestro.okchat.search.model

/**
 * Value object representing search paths
 */
data class SearchPaths(
    val paths: List<SearchPath>
) {
    companion object {
        private const val MAX_KEYWORDS = 5

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
            .take(MAX_KEYWORDS)
            .filter { it.isValid() }
            .joinToString(" OR ") { it.term }
    }

    fun isEmpty(): Boolean = paths.isEmpty()

    fun terms(): List<String> = paths.map { it.term }
}
