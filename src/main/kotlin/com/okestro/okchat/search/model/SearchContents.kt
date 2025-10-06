package com.okestro.okchat.search.model

/**
 * Value object representing search contents
 */
data class SearchContents(
    val contents: List<SearchContent>
) {
    companion object {
        private const val MAX_KEYWORDS = 5

        fun fromStrings(terms: List<String>): SearchContents {
            return SearchContents(
                terms.filter { it.isNotBlank() }
                    .mapIndexed { index, term ->
                        SearchContent(term = term, weight = (index + 1) / terms.size.toDouble())
                    }
            )
        }
    }

    /**
     * Convert to OR query (takes top 5 keywords to avoid noise)
     */
    fun toOrQuery(): String {
        return contents
            .take(MAX_KEYWORDS)
            .filter { it.isValid() }
            .joinToString(" OR ") { it.term }
    }

    fun isEmpty(): Boolean = contents.isEmpty()

    fun terms(): List<String> = contents.map { it.term }
}
