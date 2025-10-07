package com.okestro.okchat.search.model

/**
 * Value object representing search keywords.
 * Implements SearchCriteria for polymorphic handling.
 */
data class SearchKeywords(
    val keywords: List<Keyword>
) : SearchCriteria {

    override fun getSearchType(): SearchType = SearchType.KEYWORD

    override fun toQuery(): String = toOrQuery()
    companion object {
        private const val MAX_KEYWORDS = 5

        fun fromStrings(terms: List<String>): SearchKeywords {
            return SearchKeywords(
                terms.filter { it.isNotBlank() }
                    .mapIndexed { index, term ->
                        Keyword(term = term, weight = (index + 1) / terms.size.toDouble())
                    }
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

    override fun isEmpty(): Boolean = keywords.isEmpty()

    override fun size(): Int = keywords.size

    fun terms(): List<String> = keywords.map { it.term }
}
