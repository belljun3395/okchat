package com.okestro.okchat.search.application.dto

class SearchAllPathsUseCaseIn {
    override fun equals(other: Any?): Boolean = other is SearchAllPathsUseCaseIn
    override fun hashCode(): Int = 0
}

data class SearchAllPathsUseCaseOut(
    val paths: List<String>
)
