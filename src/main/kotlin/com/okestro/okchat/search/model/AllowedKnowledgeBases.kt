package com.okestro.okchat.search.model

sealed interface AllowedKnowledgeBases {
    data object All : AllowedKnowledgeBases
    data class Subset(val ids: List<Long>) : AllowedKnowledgeBases
}
