package com.okestro.okchat.knowledge.model.config

import com.okestro.okchat.email.config.EmailProperties

data class KnowledgeBaseEmailConfig(
    val emailProviders: Map<String, ProviderConfig> = emptyMap()
) {
    data class ProviderConfig(
        val type: EmailProperties.EmailProviderType,
        val host: String,
        val port: Int,
        val username: String,
        val password: String? = null,
        val enabled: Boolean = true,
        val authType: EmailProperties.AuthType = EmailProperties.AuthType.OAUTH2,
        val oauth2: OAuth2Config = OAuth2Config(),
        val mail: MailConfig = MailConfig()
    ) {
        fun toEmailProviderConfig(): EmailProperties.EmailProviderConfig {
            return EmailProperties.EmailProviderConfig(
                type = type,
                host = host,
                port = port,
                username = username,
                password = password,
                enabled = enabled,
                authType = authType,
                oauth2 = EmailProperties.OAuth2Config(
                    clientId = oauth2.clientId,
                    clientSecret = oauth2.clientSecret,
                    tenantId = oauth2.tenantId,
                    scopes = oauth2.scopes,
                    redirectUri = oauth2.redirectUri
                ),
                mail = EmailProperties.MailConfig(
                    protocol = mail.protocol,
                    timeout = mail.timeout,
                    connectionTimeout = mail.connectionTimeout,
                    debug = mail.debug,
                    debugAuth = mail.debugAuth,
                    ssl = EmailProperties.SslConfig(
                        enabled = mail.ssl.enabled,
                        trust = mail.ssl.trust
                    ),
                    properties = mail.properties
                )
            )
        }
    }

    data class OAuth2Config(
        val clientId: String = "",
        val clientSecret: String = "",
        val tenantId: String = "common",
        val scopes: List<String> = listOf(),
        val redirectUri: String = "http://localhost:8080/oauth2/callback"
    )

    data class MailConfig(
        val protocol: String = "imaps",
        val timeout: Int = 10000,
        val connectionTimeout: Int = 10000,
        val debug: Boolean = true,
        val debugAuth: Boolean = true,
        val ssl: SslConfig = SslConfig(),
        val properties: Map<String, String> = emptyMap()
    )

    data class SslConfig(
        val enabled: Boolean = true,
        val trust: String = "*"
    )
}
