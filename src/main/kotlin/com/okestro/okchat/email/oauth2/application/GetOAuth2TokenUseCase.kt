package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.GetOAuth2TokenUseCaseIn
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class GetOAuth2TokenUseCase(
    private val oAuth2TokenService: OAuth2TokenService
) {
    fun execute(input: GetOAuth2TokenUseCaseIn): Mono<String> {
        return oAuth2TokenService.getAccessToken(input.username)
    }
}
