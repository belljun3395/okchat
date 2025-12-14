package com.okestro.okchat.prompt.application.dto

data class CheckPromptExistsUseCaseIn(
    val type: String
)

data class CheckPromptExistsUseCaseOut(
    val exists: Boolean
)
