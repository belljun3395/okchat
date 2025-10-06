package com.okestro.okchat.search.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.TokenCountBatchingStrategy
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.typesense.TypesenseVectorStore
import org.springframework.ai.vectorstore.typesense.autoconfigure.TypesenseVectorStoreAutoConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.typesense.api.Client
import org.typesense.api.exceptions.TypesenseError
import org.typesense.resources.Node
import java.time.Duration

private val log = KotlinLogging.logger {}

@Configuration
@EnableAutoConfiguration(
    exclude = [TypesenseVectorStoreAutoConfiguration::class]
)
class TypesenseConfig(
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") val collectionName: String,
    @Value("\${spring.ai.vectorstore.typesense.embedding-dimension}") val embeddingDimension: Int,
    @Value("\${spring.ai.vectorstore.typesense.client.protocol}") val clientProtocol: String,
    @Value("\${spring.ai.vectorstore.typesense.client.host}") val clientHost: String,
    @Value("\${spring.ai.vectorstore.typesense.client.port}") val clientPort: Int,
    @Value("\${spring.ai.vectorstore.typesense.client.api-key}") val clientApiKey: String
) {

    @Bean
    fun typesenseClient(): Client {
        val nodes: MutableList<Node> = ArrayList()
        nodes.add(Node(clientProtocol, clientHost, clientPort.toString()))
        // Increased timeout for bulk operations (embedding generation can take time)
        val configuration = org.typesense.api.Configuration(nodes, Duration.ofSeconds(120), clientApiKey)
        return Client(configuration)
    }

    @Bean
    fun vectorStore(client: Client, embeddingModel: EmbeddingModel): VectorStore {
        log.info { "Creating TypesenseVectorStore with collection: $collectionName, embedding dimension: $embeddingDimension" }
        return TypesenseVectorStore.builder(client, embeddingModel)
            .collectionName(collectionName) // Optional: defaults to "documents"
            .embeddingDimension(embeddingDimension) // Optional: defaults to 1536
            .initializeSchema(false) // We handle schema initialization ourselves for Korean support
            .batchingStrategy(TokenCountBatchingStrategy()) // Batching for performance
            .build()
    }

    @Bean
    fun typesenseRetryTemplate(
        @Value("\${spring.ai.retry.max-attempts:5}") maxAttempts: Int,
        @Value("\${spring.ai.retry.backoff.initial-interval:2s}") initialInterval: Duration,
        @Value("\${spring.ai.retry.backoff.multiplier:2}") multiplier: Double,
        @Value("\${spring.ai.retry.backoff.max-interval:30s}") maxInterval: Duration
    ): RetryTemplate {
        log.info { "Creating RetryTemplate with maxAttempts=$maxAttempts, initialInterval=$initialInterval, multiplier=$multiplier, maxInterval=$maxInterval" }

        return RetryTemplate().apply {
            // Retry policy: retry on TypesenseError and general exceptions
            val retryPolicy = SimpleRetryPolicy(
                maxAttempts,
                mapOf(
                    TypesenseError::class.java to true,
                    Exception::class.java to true
                )
            )
            setRetryPolicy(retryPolicy)

            // Exponential backoff policy
            val backOffPolicy = ExponentialBackOffPolicy().apply {
                this.initialInterval = initialInterval.toMillis()
                this.multiplier = multiplier
                this.maxInterval = maxInterval.toMillis()
            }
            setBackOffPolicy(backOffPolicy)
        }
    }
}
