package com.okestro.okchat.search.api.internal.dto

import com.okestro.okchat.search.application.dto.MultiSearchUseCaseIn
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles

data class InternalMultiSearchRequest(
    val titles: List<String>?,
    val contents: List<String>?,
    val paths: List<String>?,
    val keywords: List<String>?,
    val topK: Int = 50
) {
    fun toUseCaseIn(): MultiSearchUseCaseIn {
        return MultiSearchUseCaseIn(
            titles = titles?.let { SearchTitles.fromStrings(it) },
            contents = contents?.let { SearchContents.fromStrings(it) },
            paths = paths?.let { SearchPaths.fromStrings(it) },
            keywords = keywords?.let { SearchKeywords.fromStrings(it) },
            topK = topK
        )
    }
}
