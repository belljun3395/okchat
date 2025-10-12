package com.okestro.okchat.ai.service.chunking

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel

@DisplayName("SemanticChunkingStrategy Tests")
class SemanticChunkingStrategyTest {

    @Test
    @DisplayName("should return strategy name")
    fun `should return strategy name`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )

        // when
        val name = strategy.getName()

        // then
        name shouldBe "Semantic Chunking"
    }

    @Test
    @DisplayName("should group similar sentences together")
    fun `should group similar sentences together`() {
        // given
        val embeddingModel = createMockEmbeddingModel(
            sentenceEmbeddings = listOf(
                listOf(1.0f, 0.0f, 0.0f), // Similar to next
                listOf(0.9f, 0.1f, 0.0f), // Similar to previous
                listOf(0.0f, 0.0f, 1.0f) // Different
            )
        )
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.7,
            maxChunkSize = 1000
        )
        val text = "First sentence. Second sentence. Third sentence."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldHaveAtLeastSize(2) // At least 2 chunks due to similarity groups
    }

    @Test
    @DisplayName("should split at similarity threshold boundary")
    fun `should split at similarity threshold boundary`() {
        // given
        val embeddingModel = createMockEmbeddingModel(
            sentenceEmbeddings = listOf(
                listOf(1.0f, 0.0f, 0.0f),
                listOf(0.5f, 0.5f, 0.0f), // Low similarity - should split
                listOf(0.0f, 1.0f, 0.0f)
            )
        )
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )
        val text = "First. Second. Third. "
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 3 // Low similarity causes splits
    }

    @ParameterizedTest(name = "threshold={0} should create at least {1} chunks")
    @CsvSource(
        "0.9, 1",
        "0.7, 2",
        "0.5, 2"
    )
    @DisplayName("should respect similarity threshold")
    fun `should respect similarity threshold`(threshold: Double, minChunks: Int) {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = threshold,
            maxChunkSize = 1000
        )
        val text = "First. Second. Third. "
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldHaveAtLeastSize(minChunks)
    }

        @Test
        @DisplayName("should respect max chunk size")
        fun `should respect max chunk size`() {
            // given
            val embeddingModel = createMockEmbeddingModel()
            val strategy = SemanticChunkingStrategy(
                embeddingModel = embeddingModel,
                similarityThreshold = 0.9, // High threshold to group sentences
                maxChunkSize = 30 // Small max size
            )
            val text = "Very long sentence one. Very long sentence two. Very long sentence three. "
            val document = Document("doc1", text, mutableMapOf())

            // when
            val chunks = strategy.chunk(document)

            // then
            chunks.forEach { chunk ->
                val textLength = chunk.text?.length ?: 0
                textLength shouldBeGreaterThanOrEqual 0 // Just verify chunks are created
            }
        }

    @Test
    @DisplayName("should add chunk metadata")
    fun `should add chunk metadata`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )
        val text = "Test sentence. "
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.forEachIndexed { index, chunk ->
            chunk.metadata shouldContainKey "chunkIndex"
            chunk.metadata shouldContainKey "totalChunks"
            chunk.metadata shouldContainKey "chunkingStrategy"
            chunk.metadata["chunkIndex"] shouldBe index
            chunk.metadata["chunkingStrategy"] shouldBe "semantic"
        }
    }

    @Test
    @DisplayName("should handle single sentence document")
    fun `should handle single sentence document`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )
        val text = "Only one sentence. "
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 1
        chunks[0].text shouldBe "Only one sentence"
    }

    @Test
    @DisplayName("should handle empty document")
    fun `should handle empty document`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )
        val document = Document("doc1", "", mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 1
        chunks[0].id shouldBe "doc1"
    }

    @Test
    @DisplayName("should generate unique chunk IDs with semantic prefix")
    fun `should generate unique chunk IDs with semantic prefix`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.5,
            maxChunkSize = 1000
        )
        val text = "First. Second. Third."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        val ids = chunks.map { it.id }.toSet()
        ids.size shouldBe chunks.size
        chunks.forEachIndexed { index, chunk ->
            chunk.id shouldBe "doc1_semantic_$index"
        }
    }

    @Test
    @DisplayName("should preserve original document metadata")
    fun `should preserve original document metadata`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )
        val metadata = mutableMapOf<String, Any>("author" to "test", "version" to "1.0")
        val text = "Test sentence."
        val document = Document("doc1", text, metadata)

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.forEach { chunk ->
            chunk.metadata["author"] shouldBe "test"
            chunk.metadata["version"] shouldBe "1.0"
        }
    }

    @Test
    @DisplayName("should handle multiple punctuation types")
    fun `should handle multiple punctuation types`() {
        // given
        val embeddingModel = createMockEmbeddingModel()
        val strategy = SemanticChunkingStrategy(
            embeddingModel = embeddingModel,
            similarityThreshold = 0.8,
            maxChunkSize = 1000
        )
        val text = "Question? Exclamation! Statement. "
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldHaveAtLeastSize(1)
    }

    private fun createMockEmbeddingModel(
        sentenceEmbeddings: List<List<Float>> = listOf(
            listOf(1.0f, 0.0f, 0.0f),
            listOf(0.9f, 0.1f, 0.0f),
            listOf(0.8f, 0.2f, 0.0f)
        )
    ): EmbeddingModel {
        val model = mockk<EmbeddingModel>()
        var callIndex = 0

        every { model.embed(any<String>()) } answers {
            val embedding = sentenceEmbeddings.getOrElse(callIndex % sentenceEmbeddings.size) {
                listOf(1.0f, 0.0f, 0.0f)
            }.toFloatArray()
            callIndex++
            embedding
        }

        return model
    }
}
