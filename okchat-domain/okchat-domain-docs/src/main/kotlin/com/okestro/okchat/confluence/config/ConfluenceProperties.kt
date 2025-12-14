package com.okestro.okchat.confluence.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "confluence")
data class ConfluenceProperties(
    val baseUrl: String,
    val auth: AuthProperties
) {
    data class AuthProperties(
        val type: AuthType = AuthType.BASIC,
        val email: String? = null,
        val apiToken: String? = null
    )

    enum class AuthType {
        BASIC
    }
}
