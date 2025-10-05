package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.model.SearchResult

/**
 * Strategy Pattern for document search.
 */
interface SearchStrategy {
    suspend fun search(query: String, topK: Int): List<SearchResult>
    fun getName(): String
}
