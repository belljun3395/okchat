package com.okestro.okchat.search.application.dto

import com.okestro.okchat.search.model.Document

data class SearchAllByPathUseCaseIn(
    val documentPath: String
)

data class SearchAllByPathUseCaseOut(
    val documents: List<Document>
)
