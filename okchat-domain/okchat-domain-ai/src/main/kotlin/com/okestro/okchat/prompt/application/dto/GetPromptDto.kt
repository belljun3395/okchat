package com.okestro.okchat.prompt.application.dto

data class GetPromptUseCaseIn(
    val type: String,
    val version: Int? = null
)

data class GetPromptUseCaseOut(
    val content: String?
)
