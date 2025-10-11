package com.okestro.okchat.email.oauth2

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.strategy.GoogleOAuth2Strategy
import com.okestro.okchat.email.oauth2.strategy.MicrosoftOAuth2Strategy
import com.okestro.okchat.email.oauth2.strategy.OAuth2ProviderStrategy
import com.okestro.okchat.email.oauth2.support.OAuth2Constants
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Service
@ConditionalOnBean(ReactiveRedisTemplate::class)
class OAuth2TokenService(
    private val emailProperties: EmailProperties,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val strategies: Map<EmailProperties.EmailProviderType, OAuth2ProviderStrategy> = mapOf(
        EmailProperties.EmailProviderType.GMAIL to GoogleOAuth2Strategy(),
        EmailProperties.EmailProviderType.OUTLOOK to MicrosoftOAuth2Strategy()
    )

    fun getAccessToken(username: String): Mono<String> {
        val providerType = resolveProviderType(username)
        val accessTokenKey = tokenKey(providerType, username)

        return reactiveRedisTemplate.opsForValue().get(accessTokenKey)
            .doOnNext { token ->
                if (token.isNullOrBlank()) {
                    logger.warn { "No valid access token found for $username(${providerType.name}). Please authenticate first." }
                }
            }
            .filter { !it.isNullOrBlank() }
            .switchIfEmpty(Mono.empty())
    }

    fun getAuthorizationUrl(username: String): String {
        val providerType = resolveProviderType(username)
        val strategy = strategies[providerType]
            ?: throw IllegalStateException("OAuth2 not supported for provider: $providerType")
        val providerConfig = emailProperties.providers.values.first { it.username.equals(username, true) }
        return strategy.buildAuthorizationUrl(username, providerConfig.oauth2)
    }

    suspend fun exchangeCodeForToken(username: String, authorizationCode: String): String {
        val providerType = resolveProviderType(username)
        val strategy = strategies[providerType]
            ?: throw IllegalStateException("OAuth2 not supported for provider: $providerType")

        val providerConfig = emailProperties.providers.values.first { it.username.equals(username, true) }
        val accessToken = strategy.exchangeToken(authorizationCode, providerConfig.oauth2)

        val accessTokenKey = tokenKey(providerType, username)
        reactiveRedisTemplate.opsForValue()
            .set(accessTokenKey, accessToken, Duration.ofMinutes(OAuth2Constants.Defaults.TOKEN_TTL_MINUTES))
            .awaitSingleOrNull()

        logger.info { "Successfully authenticated user: $username for provider $providerType, access token saved to Redis" }

        return accessToken
    }

    fun clearToken(username: String): Mono<Void> {
        val providerType = resolveProviderType(username)
        val accessTokenKey = tokenKey(providerType, username)

        return reactiveRedisTemplate.delete(accessTokenKey)
            .doOnSuccess {
                logger.info { "Cleared access token for $username(${providerType.name}) from Redis" }
            }
            .then()
    }

    private fun tokenKey(providerType: EmailProperties.EmailProviderType, username: String): String =
        OAuth2Constants.RedisKeys.accessToken(providerType.name, username)

    private fun resolveProviderType(username: String): EmailProperties.EmailProviderType {
        val matched = emailProperties.providers.values.firstOrNull { it.username.equals(username, ignoreCase = true) }
        return matched?.type
            ?: throw IllegalArgumentException("Cannot resolve provider type for username: $username. Please check email.providers configuration.")
    }
}
