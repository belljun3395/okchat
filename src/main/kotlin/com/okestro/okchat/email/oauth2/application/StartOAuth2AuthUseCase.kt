package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.StartOAuth2AuthUseCaseIn
import org.springframework.stereotype.Component

@Component
class StartOAuth2AuthUseCase(
    private val oAuth2TokenService: OAuth2TokenService
) {
    fun execute(input: StartOAuth2AuthUseCaseIn): String {
        return oAuth2TokenService.getAuthorizationUrl(input.username)
    }
}
