package com.okestro.okchat.prompt.application.dto

data class GetLatestPromptVersionUseCaseIn(
    val type: String
)

data class GetLatestPromptVersionUseCaseOut(
    val version: Int?
)
