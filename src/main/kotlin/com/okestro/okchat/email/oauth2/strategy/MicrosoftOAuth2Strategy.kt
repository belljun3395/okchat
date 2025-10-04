package com.okestro.okchat.email.oauth2.strategy

import com.microsoft.aad.msal4j.AuthorizationCodeParameters
import com.microsoft.aad.msal4j.ClientCredentialFactory
import com.microsoft.aad.msal4j.ConfidentialClientApplication
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.support.OAuth2Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.net.URI

class MicrosoftOAuth2Strategy : OAuth2ProviderStrategy {
    override fun buildAuthorizationUrl(username: String, config: EmailProperties.OAuth2Config): String =
        OAuth2AuthorizationUrlBuilder.build(
            authorizationEndpoint = OAuth2Constants.Endpoints.microsoftAuthorization(config.tenantId),
            username = username,
            config = config,
            extraParams = mapOf(
                "response_mode" to OAuth2Constants.Values.RESPONSE_MODE_QUERY
            )
        )

    override suspend fun exchangeToken(authorizationCode: String, config: EmailProperties.OAuth2Config): String =
        withContext(Dispatchers.IO) {
            val app =
                ConfidentialClientApplication
                    .builder(config.clientId, ClientCredentialFactory.createFromSecret(config.clientSecret))
                    .authority(OAuth2Constants.Endpoints.microsoftAuthority(config.tenantId))
                    .build()

            val parameters =
                AuthorizationCodeParameters
                    .builder(authorizationCode, URI(config.redirectUri))
                    .scopes(config.scopes.toSet())
                    .build()

            val result = app.acquireToken(parameters).await()
            result.accessToken()
        }
}
