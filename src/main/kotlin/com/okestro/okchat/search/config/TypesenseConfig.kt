package com.okestro.okchat.search.config

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.TokenCountBatchingStrategy
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.typesense.TypesenseVectorStore
import org.springframework.ai.vectorstore.typesense.autoconfigure.TypesenseVectorStoreAutoConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.typesense.api.Client
import org.typesense.resources.Node
import java.time.Duration

@Configuration
@EnableAutoConfiguration(
    exclude = [TypesenseVectorStoreAutoConfiguration::class]
)
class TypesenseConfig(
    @Value("\${spring.ai.vectorstore.typesense.initialize-schema}") val initializeSchema: Boolean,
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
        val configuration = org.typesense.api.Configuration(nodes, Duration.ofSeconds(5), clientApiKey)
        return Client(configuration)
    }

    @Bean
    fun vectorStore(client: Client, embeddingModel: EmbeddingModel): VectorStore {
        return TypesenseVectorStore.builder(client, embeddingModel)
            .collectionName(collectionName) // Optional: defaults to "documents"
            .embeddingDimension(embeddingDimension) // Optional: defaults to 1536
            .initializeSchema(initializeSchema) // Optional: defaults to false
            .batchingStrategy(TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
            .build()
    }
}
