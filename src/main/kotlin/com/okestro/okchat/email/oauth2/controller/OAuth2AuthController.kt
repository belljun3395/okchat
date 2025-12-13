package com.okestro.okchat.email.oauth2.controller

import com.okestro.okchat.email.oauth2.OAuth2TokenService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

private val logger = KotlinLogging.logger {}

@RestController
class OAuth2AuthController(
    private val oauth2TokenService: OAuth2TokenService
) {
    /**
     * Start OAuth2 authentication
     * @see <a href="http://localhost:8080/api/email/oauth2/authenticate?username=your@email.com"> Start Authentication </a>
     */
    @GetMapping("/api/email/oauth2/authenticate")
    fun authenticate(
        @RequestParam username: String,
        exchange: ServerWebExchange
    ): Mono<Void> {
        val authUrl = oauth2TokenService.getAuthorizationUrl(username)
        logger.info { "Redirecting to OAuth2 login for $username" }

        exchange.response.statusCode = HttpStatus.FOUND
        exchange.response.headers.location = URI.create(authUrl)
        return exchange.response.setComplete()
    }



    /**
     * Check stored token
     * @see <a href="http://localhost:8080/api/email/oauth2/token?username=your@email.com> Check Token </a>
     */
    @GetMapping("/api/email/oauth2/token")
    fun getToken(
        @RequestParam username: String
    ): Mono<Map<String, String>> =
        oauth2TokenService.getAccessToken(username)
            .map { token ->
                mapOf(
                    "status" to "success",
                    "hasToken" to "true",
                    "tokenPreview" to token.take(20) + "..."
                )
            }
            .switchIfEmpty(
                Mono.just(
                    mapOf(
                        "status" to "error",
                        "hasToken" to "false",
                        "message" to "No token found. Please authenticate first."
                    )
                )
            )

    /**
     * Delete token
     * @see <a href="http://localhost:8080 /api/email/oauth2/token?username=your@email.com"> Clear Token </a>
     */
    @GetMapping("/api/email/oauth2/clear")
    fun clearToken(
        @RequestParam username: String
    ): Mono<Map<String, String>> =
        oauth2TokenService.clearToken(username)
            .thenReturn(
                mapOf(
                    "status" to "success",
                    "message" to "Token cleared for $username"
                )
            )
}
