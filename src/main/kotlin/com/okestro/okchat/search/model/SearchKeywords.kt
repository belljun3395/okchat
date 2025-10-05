package com.okestro.okchat.search.model

/**
 * Value object representing search keywords
 */
data class SearchKeywords(
    val keywords: List<Keyword>
) {
    companion object {
        private const val MAX_KEYWORDS = 5

        fun fromStrings(terms: List<String>): SearchKeywords {
            return SearchKeywords(
                terms.filter { it.isNotBlank() }
                    .map { Keyword.of(it) }
            )
        }
    }

    /**
     * Convert to OR query (takes top 5 keywords to avoid noise)
     */
    fun toOrQuery(): String {
        return keywords
            .take(MAX_KEYWORDS)
            .filter { it.isValid() }
            .joinToString(" OR ") { it.term }
    }

    fun isEmpty(): Boolean = keywords.isEmpty()

    fun terms(): List<String> = keywords.map { it.term }
}
