package com.okestro.okchat.email.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "email")
data class EmailProperties(
    val providers: Map<String, EmailProviderConfig> = emptyMap(),
    val polling: PollingConfig = PollingConfig(),
    val mail: MailConfig = MailConfig()
) {
    data class EmailProviderConfig(
        val type: EmailProviderType,
        val host: String,
        val port: Int,
        val username: String,
        val password: String? = null,
        val enabled: Boolean = true,
        val authType: AuthType = AuthType.OAUTH2,
        val oauth2: OAuth2Config = OAuth2Config(),
        val mail: MailConfig = MailConfig()
    )

    data class PollingConfig(
        val interval: Long = 60000,
        val initialDelay: Long = 10000
    )

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

    enum class EmailProviderType {
        OUTLOOK,
        GMAIL
    }

    enum class AuthType {
        OAUTH2
    }
}
