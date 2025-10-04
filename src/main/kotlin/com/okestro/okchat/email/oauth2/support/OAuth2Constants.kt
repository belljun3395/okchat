package com.okestro.okchat.email.oauth2.support

/**
 * Constants for OAuth2 authentication
 */
object OAuth2Constants {
    /**
     * OAuth2 authorization and token endpoints
     */
    object Endpoints {
        const val GOOGLE_AUTHORIZATION = "https://accounts.google.com/o/oauth2/v2/auth"
        const val GOOGLE_TOKEN = "https://oauth2.googleapis.com/token"

        fun microsoftAuthorization(tenantId: String) =
            "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/authorize"

        fun microsoftAuthority(tenantId: String) =
            "https://login.microsoftonline.com/$tenantId"
    }

    /**
     * OAuth2 parameter values
     */
    object Values {
        const val XOAUTH2 = "XOAUTH2"
        const val ACCESS_TYPE_OFFLINE = "offline"
        const val PROMPT_CONSENT = "consent"
        const val RESPONSE_MODE_QUERY = "query"
    }

    /**
     * Default configuration values
     */
    object Defaults {
        const val TOKEN_TTL_MINUTES = 50L
    }

    /**
     * Redis key patterns for token storage
     */
    object RedisKeys {
        private const val ACCESS_TOKEN_PREFIX = "oauth2:access_token"

        fun accessToken(providerType: String, username: String) =
            "$ACCESS_TOKEN_PREFIX:$providerType:$username"
    }
}
