package com.okestro.okchat.permission.application.dto

import com.okestro.okchat.search.model.SearchResult

data class FilterSearchResultsUseCaseIn(
    val results: List<SearchResult>,
    val userId: Long
)

data class FilterSearchResultsUseCaseOut(
    val filteredResults: List<SearchResult>
)
