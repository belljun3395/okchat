package com.okestro.okchat.prompt.application.dto

import com.okestro.okchat.prompt.model.entity.Prompt

data class UpdatePromptUseCaseIn(
    val type: String,
    val content: String
)

data class UpdatePromptUseCaseOut(
    val prompt: Prompt
)
