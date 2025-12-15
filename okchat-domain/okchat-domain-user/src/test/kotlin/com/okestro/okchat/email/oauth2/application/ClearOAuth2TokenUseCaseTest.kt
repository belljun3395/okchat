package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.ClearOAuth2TokenUseCaseIn
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ClearOAuth2TokenUseCaseTest : BehaviorSpec({

    val oAuth2TokenService = mockk<OAuth2TokenService>()
    val useCase = ClearOAuth2TokenUseCase(oAuth2TokenService)

    afterEach {
        clearAllMocks()
    }

    given("OAuth2 token clearing is requested") {
        val username = "test@example.com"
        val input = ClearOAuth2TokenUseCaseIn(username)

        `when`("Service successfully clears token") {
            every { oAuth2TokenService.clearToken(username) } returns Mono.empty()

            val result = useCase.execute(input)

            then("Token is cleared successfully") {
                StepVerifier.create(result)
                    .verifyComplete()

                verify(exactly = 1) { oAuth2TokenService.clearToken(username) }
            }
        }
    }
})
