package com.okestro.okchat.prompt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger {}

@Service
class PromptCacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    companion object {
        private const val PROMPT_LATEST_CACHE_PREFIX = "prompt:latest:"
        private val CACHE_TTL = Duration.ofHours(24)
    }

    suspend fun getLatestPrompt(type: String): String? {
        return try {
            val key = "$PROMPT_LATEST_CACHE_PREFIX$type"
            val cached = redisTemplate.opsForValue().get(key)
            log.debug { "Cache hit for prompt: $key" }
            cached.awaitSingleOrNull()
        } catch (e: Exception) {
            log.warn(e) { "Failed to get latest prompt from cache: type=$type" }
            null
        }
    }

    suspend fun cacheLatestPrompt(type: String, content: String) {
        try {
            val key = "$PROMPT_LATEST_CACHE_PREFIX$type"
            redisTemplate.opsForValue().set(key, content, CACHE_TTL).awaitSingleOrNull()
            log.debug { "Cached latest prompt: $key" }
        } catch (e: Exception) {
            log.warn(e) { "Failed to cache latest prompt: type=$type" }
        }
    }

    suspend fun evictLatestPromptCache(type: String) {
        try {
            val key = "$PROMPT_LATEST_CACHE_PREFIX$type"
            redisTemplate.delete(key).awaitSingleOrNull()
            log.debug { "Evicted latest prompt cache: $key" }
        } catch (e: Exception) {
            log.warn(e) { "Failed to evict latest prompt cache: type=$type" }
        }
    }
}
