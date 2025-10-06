package com.okestro.okchat.search.model

/**
 * Value object representing search titles
 */
data class SearchTitles(
    val titles: List<SearchTitle>
) {
    companion object {
        private const val MAX_KEYWORDS = 5

        fun fromStrings(terms: List<String>): SearchTitles {
            return SearchTitles(
                terms.filter { it.isNotBlank() }
                    .mapIndexed { index, term ->
                        SearchTitle(term = term, weight = (index + 1) / terms.size.toDouble())
                    }
            )
        }
    }

    /**
     * Convert to OR query (takes top 5 keywords to avoid noise)
     */
    fun toOrQuery(): String {
        return titles
            .take(MAX_KEYWORDS)
            .filter { it.isValid() }
            .joinToString(" OR ") { it.term }
    }

    fun isEmpty(): Boolean = titles.isEmpty()

    fun terms(): List<String> = titles.map { it.term }
}
