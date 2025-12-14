package com.okestro.okchat.search.application.dto

import com.okestro.okchat.search.model.AllowedKnowledgeBases

data class SearchAllPathsUseCaseIn(
    val allowedKbIds: AllowedKnowledgeBases
)
