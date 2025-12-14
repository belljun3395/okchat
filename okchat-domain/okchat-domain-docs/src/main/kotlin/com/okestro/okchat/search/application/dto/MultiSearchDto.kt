package com.okestro.okchat.search.application.dto

import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles

data class MultiSearchUseCaseIn(
    val titles: SearchTitles?,
    val contents: SearchContents?,
    val paths: SearchPaths?,
    val keywords: SearchKeywords?,
    val topK: Int = 50
)

data class MultiSearchUseCaseOut(
    val result: MultiSearchResult
)
