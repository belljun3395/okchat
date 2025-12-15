package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.GetOAuth2TokenUseCaseIn
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class GetOAuth2TokenUseCaseTest : BehaviorSpec({

    val oAuth2TokenService = mockk<OAuth2TokenService>()
    val useCase = GetOAuth2TokenUseCase(oAuth2TokenService)

    afterEach {
        clearAllMocks()
    }

    given("OAuth2 access token retrieval is requested") {
        val username = "test@example.com"
        val input = GetOAuth2TokenUseCaseIn(username)
        val expectedToken = "ya29.a0AfH6SMBx..."

        `when`("Token exists in cache") {
            every { oAuth2TokenService.getAccessToken(username) } returns Mono.just(expectedToken)

            val result = useCase.execute(input)

            then("Access token is returned") {
                StepVerifier.create(result)
                    .expectNext(expectedToken)
                    .verifyComplete()

                verify(exactly = 1) { oAuth2TokenService.getAccessToken(username) }
            }
        }

        `when`("Token does not exist") {
            every { oAuth2TokenService.getAccessToken(username) } returns Mono.empty()

            val result = useCase.execute(input)

            then("Empty Mono is returned") {
                StepVerifier.create(result)
                    .verifyComplete()
            }
        }
    }
})
