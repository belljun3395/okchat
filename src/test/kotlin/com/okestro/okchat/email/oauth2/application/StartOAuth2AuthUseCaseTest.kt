package com.okestro.okchat.email.oauth2.application

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.oauth2.application.dto.StartOAuth2AuthUseCaseIn
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class StartOAuth2AuthUseCaseTest : BehaviorSpec({

    val oAuth2TokenService = mockk<OAuth2TokenService>()
    val useCase = StartOAuth2AuthUseCase(oAuth2TokenService)

    afterEach {
        clearAllMocks()
    }

    given("OAuth2 authorization URL is requested") {
        val username = "test@example.com"
        val input = StartOAuth2AuthUseCaseIn(username)
        val expectedUrl = "https://accounts.google.com/o/oauth2/v2/auth?..."

        `when`("Service returns authorization URL") {
            every { oAuth2TokenService.getAuthorizationUrl(username) } returns expectedUrl

            val result = useCase.execute(input)

            then("Authorization URL is returned") {
                result shouldBe expectedUrl
                verify(exactly = 1) { oAuth2TokenService.getAuthorizationUrl(username) }
            }
        }
    }
})
