package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class EmailProviderFactory(
    private val oauth2TokenService: OAuth2TokenService,
    private val emailProperties: EmailProperties
) {
    fun createProvider(
        name: String,
        config: EmailProperties.EmailProviderConfig
    ): EmailProvider? {
        if (!config.enabled) {
            logger.info { "Email provider '$name' is disabled" }
            return null
        }

        return when (config.type) {
            EmailProperties.EmailProviderType.OUTLOOK -> OutlookEmailProvider(config, emailProperties, oauth2TokenService)
            EmailProperties.EmailProviderType.GMAIL -> GmailEmailProvider(config, emailProperties, oauth2TokenService)
        }
    }
}
