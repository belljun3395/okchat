package com.okestro.okchat.prompt.application.dto

data class DeactivatePromptUseCaseIn(
    val type: String,
    val version: Int
)

data class DeactivatePromptUseCaseOut(
    val success: Boolean
)
