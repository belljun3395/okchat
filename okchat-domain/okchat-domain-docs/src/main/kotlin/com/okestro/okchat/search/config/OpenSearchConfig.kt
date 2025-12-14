package com.okestro.okchat.search.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.opensearch.client.RestClient
import org.opensearch.client.RestClientBuilder
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.rest_client.RestClientTransport
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * OpenSearch configuration with proper separation of concerns.
 * * This configuration creates:
 * 1. RestClient - Low-level HTTP client
 * 2. RestHighLevelClient - High-level REST client for backwards compatibility
 * 3. OpenSearchClient - New Java client
 * 4. RetryTemplate - For resilient operations
 */
@Configuration
@EnableAutoConfiguration(
    exclude = [
        ElasticsearchDataAutoConfiguration::class,
        ElasticsearchRestClientAutoConfiguration::class
    ]
)
class OpenSearchConfig(
    @Value("\${spring.ai.vectorstore.opensearch.host:localhost}") val host: String,
    @Value("\${spring.ai.vectorstore.opensearch.port:9200}") val port: Int,
    @Value("\${spring.ai.vectorstore.opensearch.scheme:http}") val scheme: String,
    @Value("\${spring.ai.vectorstore.opensearch.username:}") val username: String,
    @Value("\${spring.ai.vectorstore.opensearch.password:}") val password: String
) {

    /**
     * Low-level REST client bean.
     * This is the foundation for both high-level clients.
     */
    @Bean
    fun restClient(): RestClient {
        log.info { "Creating OpenSearch RestClient: $scheme://$host:$port" }

        val builder: RestClientBuilder = RestClient.builder(HttpHost(host, port, scheme))

        // Add authentication if credentials are provided
        if (username.isNotBlank() && password.isNotBlank()) {
            log.info { "Configuring OpenSearch with authentication for user: $username" }
            val credentialsProvider = BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
            }
            builder.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            }
        }

        // Configure timeouts
        builder.setRequestConfigCallback { requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout(5000)
                .setSocketTimeout(60000)
        }

        return builder.build()
    }

    /**
     * High-level REST client for legacy OpenSearch operations.
     * Used for operations not yet available in the new Java client.
     */
    @Bean
    fun restHighLevelClient(): RestHighLevelClient {
        log.info { "Creating OpenSearch RestHighLevelClient" }
        return RestHighLevelClient(
            RestClient.builder(HttpHost(host, port, scheme))
        )
    }

    /**
     * New Java client for OpenSearch.
     * This is the recommended client for new implementations.
     */
    @Bean
    fun openSearchClient(restClient: RestClient): OpenSearchClient {
        log.info { "Creating OpenSearch Java Client" }
        val transport = RestClientTransport(restClient, JacksonJsonpMapper())
        return OpenSearchClient(transport)
    }

    /**
     * Retry template for resilient OpenSearch operations.
     * Configured with exponential backoff.
     */
    @Bean
    fun openSearchRetryTemplate(
        @Value("\${spring.ai.retry.max-attempts}") maxAttempts: Int,
        @Value("\${spring.ai.retry.backoff.initial-interval}") initialInterval: Duration,
        @Value("\${spring.ai.retry.backoff.multiplier}") multiplier: Double,
        @Value("\${spring.ai.retry.backoff.max-interval}") maxInterval: Duration
    ): RetryTemplate {
        log.info { "Creating OpenSearch RetryTemplate: maxAttempts=$maxAttempts, initialInterval=$initialInterval" }

        return RetryTemplate().apply {
            // Retry on any exception
            val retryPolicy = SimpleRetryPolicy(
                maxAttempts,
                mapOf(Exception::class.java to true)
            )
            setRetryPolicy(retryPolicy)

            // Exponential backoff
            val backOffPolicy = ExponentialBackOffPolicy().apply {
                this.initialInterval = initialInterval.toMillis()
                this.multiplier = multiplier
                this.maxInterval = maxInterval.toMillis()
            }
            setBackOffPolicy(backOffPolicy)
        }
    }
}
