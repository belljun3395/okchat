package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.ExchangeOAuth2CodeUseCaseIn
import org.springframework.stereotype.Component

@Component
class ExchangeOAuth2CodeUseCase(
    private val oAuth2TokenService: OAuth2TokenService
) {
    suspend fun execute(input: ExchangeOAuth2CodeUseCaseIn): String {
        return oAuth2TokenService.exchangeCodeForToken(input.username, input.code)
    }
}
