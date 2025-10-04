package com.okestro.okchat.email.oauth2.strategy

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.support.OAuth2Constants
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

class GoogleOAuth2Strategy : OAuth2ProviderStrategy {
    override fun buildAuthorizationUrl(username: String, config: EmailProperties.OAuth2Config): String =
        OAuth2AuthorizationUrlBuilder.build(
            authorizationEndpoint = OAuth2Constants.Endpoints.GOOGLE_AUTHORIZATION,
            username = username,
            config = config,
            extraParams = mapOf(
                "access_type" to OAuth2Constants.Values.ACCESS_TYPE_OFFLINE,
                "prompt" to OAuth2Constants.Values.PROMPT_CONSENT
            )
        )

    override suspend fun exchangeToken(authorizationCode: String, config: EmailProperties.OAuth2Config): String {
        val webClient = WebClient.create()

        val response = webClient
            .post()
            .uri(OAuth2Constants.Endpoints.GOOGLE_TOKEN)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(
                "code=$authorizationCode" +
                    "&client_id=${config.clientId}" +
                    "&client_secret=${config.clientSecret}" +
                    "&redirect_uri=${config.redirectUri}" +
                    "&grant_type=authorization_code"
            )
            .retrieve()
            .awaitBody<Map<String, Any>>()

        return response["access_token"] as? String
            ?: throw IllegalStateException("No access token in Google response")
    }
}
