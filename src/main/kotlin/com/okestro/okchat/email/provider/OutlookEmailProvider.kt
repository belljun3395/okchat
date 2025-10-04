package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService

class OutlookEmailProvider(
    config: EmailProperties.EmailProviderConfig,
    emailProperties: EmailProperties,
    private val oauth2TokenService: OAuth2TokenService? = null
) : AbstractEmailProvider(config, emailProperties) {

    override suspend fun getPassword(): String? =
        when (config.authType) {
            EmailProperties.AuthType.OAUTH2 -> {
                oauth2TokenService?.getAccessToken(config.username)?.block()
                    ?: throw IllegalStateException("OAuth2 token not available for ${config.username}. Please authenticate first.")
            }
        }
}
