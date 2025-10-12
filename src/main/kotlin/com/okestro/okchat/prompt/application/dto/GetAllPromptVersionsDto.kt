package com.okestro.okchat.prompt.application.dto

import com.okestro.okchat.prompt.model.Prompt

data class GetAllPromptVersionsUseCaseIn(
    val type: String
)

data class GetAllPromptVersionsUseCaseOut(
    val prompts: List<Prompt>
)
