package com.okestro.okchat.search.model

/**
 * Value object representing a single content term
 */
data class SearchContent(
    val term: String,
    val weight: Double = 1.0
) {
    companion object {
        fun of(term: String): SearchContent = SearchContent(term)
    }

    fun isValid(): Boolean = term.isNotBlank()
}
