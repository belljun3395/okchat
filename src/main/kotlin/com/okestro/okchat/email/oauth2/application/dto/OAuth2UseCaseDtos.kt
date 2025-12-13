package com.okestro.okchat.email.oauth2.application.dto

data class StartOAuth2AuthUseCaseIn(
    val username: String
)

data class GetOAuth2TokenUseCaseIn(
    val username: String
)

data class ExchangeOAuth2CodeUseCaseIn(
    val username: String,
    val code: String
)

data class ClearOAuth2TokenUseCaseIn(
    val username: String
)
