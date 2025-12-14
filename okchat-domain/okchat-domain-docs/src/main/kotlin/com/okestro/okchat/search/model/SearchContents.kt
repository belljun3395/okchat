package com.okestro.okchat.search.model

/**
 * Value object representing search contents.
 * Implements SearchCriteria for polymorphic handling.
 */
data class SearchContents(
    val contents: List<SearchContent>
) : SearchCriteria {

    override fun getSearchType(): SearchType = SearchType.CONTENT

    override fun toQuery(): String = toOrQuery()
    companion object {
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
            .filter { it.isValid() }
            .joinToString(" OR ") { it.term }
    }

    override fun isEmpty(): Boolean = contents.isEmpty()

    override fun size(): Int = contents.size

    fun terms(): List<String> = contents.map { it.term }
}
