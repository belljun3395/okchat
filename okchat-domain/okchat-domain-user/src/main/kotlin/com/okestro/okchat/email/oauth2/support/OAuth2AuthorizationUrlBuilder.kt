package com.okestro.okchat.email.oauth2.support

import com.okestro.okchat.email.config.EmailProperties
import java.net.URLEncoder

object OAuth2AuthorizationUrlBuilder {
    fun build(
        authorizationEndpoint: String,
        username: String,
        config: EmailProperties.OAuth2Config,
        extraParams: Map<String, String> = emptyMap()
    ): String {
        val scopes = config.scopes.joinToString(" ")
        val encodedRedirectUri = URLEncoder.encode(config.redirectUri, "UTF-8")
        val encodedScopes = URLEncoder.encode(scopes, "UTF-8")
        val encodedUsername = URLEncoder.encode(username, "UTF-8")

        val baseParams = mapOf(
            "client_id" to config.clientId,
            "response_type" to "code",
            "redirect_uri" to encodedRedirectUri,
            "scope" to encodedScopes,
            "login_hint" to encodedUsername,
            "state" to encodedUsername
        )

        val allParams = baseParams + extraParams

        val query = allParams.entries.joinToString("&") { (k, v) -> "$k=$v" }
        return "$authorizationEndpoint?$query"
    }
}
