package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.support.MailPropertiesBuilder
import kotlinx.coroutines.reactor.awaitSingleOrNull

class GmailEmailProvider(
    config: EmailProperties.EmailProviderConfig,
    emailProperties: EmailProperties,
    private val oauth2TokenService: OAuth2TokenService? = null
) : AbstractEmailProvider(config, emailProperties) {

    override fun MailPropertiesBuilder.applyProviderSpecificProperties() {
        if (config.authType == EmailProperties.AuthType.OAUTH2) {
            customProperty("mail.imaps.sasl.mechanisms.oauth2.oauthToken", "true")
        }
    }

    override suspend fun getPassword(): String =
        when (config.authType) {
            EmailProperties.AuthType.OAUTH2 -> {
                oauth2TokenService?.getAccessToken(config.username)?.awaitSingleOrNull()
                    ?: throw IllegalStateException("OAuth2 token not available for ${config.username}. Please authenticate first.")
            }
        }
}
