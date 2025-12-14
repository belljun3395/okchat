package com.okestro.okchat.search.model

/**
 * Value object representing a single path
 */
data class SearchPath(
    val term: String,
    val weight: Double = 1.0
) {
    companion object {
        fun of(term: String): SearchPath = SearchPath(term)
    }

    fun isValid(): Boolean = term.isNotBlank()
}
