package com.okestro.okchat.search.model

/**
 * Value object representing a single title
 */
data class SearchTitle(
    val term: String,
    val weight: Double = 1.0
) {
    companion object {
        fun of(term: String): SearchTitle = SearchTitle(term)
    }

    fun isValid(): Boolean = term.isNotBlank()
}
