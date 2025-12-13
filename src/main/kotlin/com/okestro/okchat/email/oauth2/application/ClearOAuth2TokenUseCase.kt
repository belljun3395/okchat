package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.ClearOAuth2TokenUseCaseIn
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ClearOAuth2TokenUseCase(
    private val oAuth2TokenService: OAuth2TokenService
) {
    fun execute(input: ClearOAuth2TokenUseCaseIn): Mono<Void> {
        return oAuth2TokenService.clearToken(input.username)
    }
}
