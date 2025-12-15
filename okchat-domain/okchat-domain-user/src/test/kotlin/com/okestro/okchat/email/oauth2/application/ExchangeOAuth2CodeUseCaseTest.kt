package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.ExchangeOAuth2CodeUseCaseIn
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class ExchangeOAuth2CodeUseCaseTest : BehaviorSpec({

    val oAuth2TokenService = mockk<OAuth2TokenService>()
    val useCase = ExchangeOAuth2CodeUseCase(oAuth2TokenService)

    afterEach {
        clearAllMocks()
    }

    given("OAuth2 authorization code exchange is requested") {
        val username = "test@example.com"
        val code = "4/0AY0e-g7X..."
        val expectedToken = "ya29.a0AfH6SMBx..."
        val input = ExchangeOAuth2CodeUseCaseIn(username, code)

        `when`("Service successfully exchanges code for token") {
            coEvery { oAuth2TokenService.exchangeCodeForToken(username, code) } returns expectedToken

            val result = useCase.execute(input)

            then("Token is returned successfully") {
                result == expectedToken
                coVerify(exactly = 1) { oAuth2TokenService.exchangeCodeForToken(username, code) }
            }
        }

        `when`("Service fails to exchange code") {
            val exception = RuntimeException("Exchange failed")
            coEvery { oAuth2TokenService.exchangeCodeForToken(username, code) } throws exception

            then("Error is propagated") {
                try {
                    useCase.execute(input)
                    throw AssertionError("Expected exception to be thrown")
                } catch (_: RuntimeException) {
                    // Expected
                }
            }
        }
    }
})
