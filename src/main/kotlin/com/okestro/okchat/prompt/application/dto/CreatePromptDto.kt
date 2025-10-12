package com.okestro.okchat.prompt.application.dto

import com.okestro.okchat.prompt.model.Prompt

data class CreatePromptUseCaseIn(
    val type: String,
    val content: String
)

data class CreatePromptUseCaseOut(
    val prompt: Prompt
)
