package com.okestro.okchat.email.oauth2.strategy

import com.okestro.okchat.email.config.EmailProperties

interface OAuth2ProviderStrategy {
    fun buildAuthorizationUrl(username: String, config: EmailProperties.OAuth2Config): String

    suspend fun exchangeToken(authorizationCode: String, config: EmailProperties.OAuth2Config): String
}
