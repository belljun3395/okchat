package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import com.okestro.okchat.email.support.MailPropertiesBuilder
import kotlinx.coroutines.reactor.awaitSingleOrNull

class OutlookEmailProvider(
    config: EmailProperties.EmailProviderConfig,
    emailProperties: EmailProperties,
    private val oauth2TokenService: OAuth2TokenService? = null
) : AbstractEmailProvider(config, emailProperties) {

    override fun MailPropertiesBuilder.applyProviderSpecificProperties() {
        if (config.authType == EmailProperties.AuthType.OAUTH2) {
            // Microsoft/Outlook OAuth2 IMAP requires XOAUTH2 SASL mechanism
            customProperty("mail.imaps.auth.mechanisms", "XOAUTH2")
            customProperty("mail.imaps.sasl.enable", "true")
            customProperty("mail.imaps.sasl.mechanisms", "XOAUTH2")
            customProperty("mail.imaps.auth.login.disable", "true")
            customProperty("mail.imaps.auth.plain.disable", "true")
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
