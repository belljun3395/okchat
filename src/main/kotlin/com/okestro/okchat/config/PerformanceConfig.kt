package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.Duration
import java.util.concurrent.Executor

private val log = KotlinLogging.logger {}

/**
 * Performance optimization configuration for the application
 * Configures caching, async processing, and thread pools
 */
@Configuration
@EnableCaching
@EnableAsync
@EnableConfigurationProperties(PerformanceProperties::class)
class PerformanceConfig {

    /**
     * Configure async task executor with optimized thread pool settings
     */
    @Bean(name = ["asyncTaskExecutor"])
    fun asyncTaskExecutor(properties: PerformanceProperties): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = properties.async.corePoolSize
        executor.maxPoolSize = properties.async.maxPoolSize
        executor.queueCapacity = properties.async.queueCapacity
        executor.threadNamePrefix = "async-"
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(properties.async.awaitTerminationSeconds)
        executor.initialize()

        log.info { "Configured async executor: core=${properties.async.corePoolSize}, max=${properties.async.maxPoolSize}, queue=${properties.async.queueCapacity}" }
        return executor
    }

    /**
     * Configure Redis cache manager with TTL and serialization settings
     */
    @Bean
    fun cacheManager(
        redisConnectionFactory: RedisConnectionFactory,
        properties: PerformanceProperties
    ): CacheManager {
        val cacheConfigurations = mutableMapOf<String, RedisCacheConfiguration>()
        
        // Configure cache-specific TTLs
        properties.cache.ttls.forEach { (cacheName, ttlMinutes) ->
            cacheConfigurations[cacheName] = createCacheConfiguration(Duration.ofMinutes(ttlMinutes))
        }

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(createCacheConfiguration(Duration.ofMinutes(properties.cache.defaultTtlMinutes)))
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    private fun createCacheConfiguration(ttl: Duration): RedisCacheConfiguration {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues()
    }
}

/**
 * Performance-related configuration properties
 */
@ConfigurationProperties(prefix = "performance")
data class PerformanceProperties(
    val async: AsyncProperties = AsyncProperties(),
    val cache: CacheProperties = CacheProperties()
) {
    data class AsyncProperties(
        val corePoolSize: Int = 10,
        val maxPoolSize: Int = 20,
        val queueCapacity: Int = 500,
        val awaitTerminationSeconds: Int = 60
    )

    data class CacheProperties(
        val defaultTtlMinutes: Long = 60,
        val ttls: Map<String, Long> = mapOf(
            "users" to 120,
            "permissions" to 30,
            "prompts" to 60,
            "documents" to 120,
            "searchResults" to 15
        )
    )
}