package com.okestro.okchat.search.config

import io.mockk.*
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ErrorResponse
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.CreateIndexResponse
import org.opensearch.client.opensearch.indices.GetIndexRequest
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient
import org.opensearch.client.util.ObjectBuilder
import java.util.function.Function

class OpenSearchSchemaInitializerTest {

    private val openSearchClient = mockk<OpenSearchClient>()
    private val indicesClient = mockk<OpenSearchIndicesClient>()

    private val initializer = OpenSearchSchemaInitializer(
        openSearchClient = openSearchClient,
        indexName = "test-index",
        embeddingDimension = 1536,
        host = "localhost",
        port = 9200,
        scheme = "http"
    )

    @Test
    fun `initializeKoreanSupportedSchema should create index if it does not exist`() {
        // Given
        every { openSearchClient.indices() } returns indicesClient

        // Mock get index throwing 404
        val notFoundException = OpenSearchException(
            ErrorResponse.Builder().status(404).error { e -> e.reason("Not found").type("index_not_found_exception") }.build()
        )

        every { indicesClient.get(any<Function<GetIndexRequest.Builder, org.opensearch.client.util.ObjectBuilder<GetIndexRequest>>>()) } throws notFoundException

        // Mock create index
        val createResponse = mockk<CreateIndexResponse>()
        every { indicesClient.create(any<Function<CreateIndexRequest.Builder, org.opensearch.client.util.ObjectBuilder<CreateIndexRequest>>>()) } returns createResponse

        // When
        initializer.initializeKoreanSupportedSchema()

        // Then
        verify { indicesClient.create(any<Function<CreateIndexRequest.Builder, org.opensearch.client.util.ObjectBuilder<CreateIndexRequest>>>()) }
    }
}
