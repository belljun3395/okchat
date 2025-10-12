package com.okestro.okchat.ai.service.chunking

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.ai.document.Document

@DisplayName("RecursiveCharacterStrategy Tests")
class RecursiveCharacterStrategyTest {

    @Test
    @DisplayName("should return strategy name")
    fun `should return strategy name`() {
        // given
        val strategy = createStrategy()

        // when
        val name = strategy.getName()

        // then
        name shouldBe "Recursive Character Splitter"
    }

    @Test
    @DisplayName("should chunk document into multiple parts")
    fun `should chunk document into multiple parts`() {
        // given
        val strategy = createStrategy(chunkSize = 100, chunkOverlap = 20)
        val longText = "This is a test sentence. ".repeat(20) // 500 chars
        val document = Document("doc1", longText, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
        chunks.size shouldBe 5 // 500 chars / 100 chunk size = 5 chunks (approximately)
    }

    @Test
    @DisplayName("should preserve document metadata")
    fun `should preserve document metadata`() {
        // given
        val strategy = createStrategy()
        val metadata = mutableMapOf<String, Any>("key" to "value", "author" to "test")
        val document = Document("doc1", "Test content", metadata)

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.forEach { chunk ->
            chunk.metadata["key"] shouldBe "value"
            chunk.metadata["author"] shouldBe "test"
        }
    }

    @Test
    @DisplayName("should handle short document without chunking")
    fun `should handle short document without chunking`() {
        // given
        val strategy = createStrategy(chunkSize = 1000)
        val shortText = "This is a short document."
        val document = Document("doc1", shortText, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 1
        chunks[0].text shouldBe shortText
    }

    @Test
    @DisplayName("should handle empty document")
    fun `should handle empty document`() {
        // given
        val strategy = createStrategy()
        val document = Document("doc1", "", mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 1
    }

    @ParameterizedTest(name = "should chunk with overlap={1}, expected at least {2} chunks")
    @CsvSource(
        "0, 0, 3",
        "50, 50, 4",
        "100, 100, 5"
    )
    @DisplayName("should apply chunk overlap correctly")
    fun `should apply chunk overlap correctly`(chunkSize: Int, overlap: Int, minExpectedChunks: Int) {
        // given
        val strategy = createStrategy(chunkSize = chunkSize, chunkOverlap = overlap)
        val text = "Word ".repeat(100) // 500 chars
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.size shouldBe minExpectedChunks
    }

    @Test
    @DisplayName("should respect minChunkLengthToEmbed setting")
    fun `should respect minChunkLengthToEmbed setting`() {
        // given
        val strategy = createStrategy(
            chunkSize = 100,
            minChunkLengthToEmbed = 50
        )
        val text = "This is a test. " + "Word ".repeat(50)
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.forEach { chunk ->
            (chunk.text?.length ?: 0) shouldBe 0 // All chunks should meet minimum length
        }
    }

    @Test
    @DisplayName("should generate unique chunk IDs")
    fun `should generate unique chunk IDs`() {
        // given
        val strategy = createStrategy(chunkSize = 50)
        val text = "Text ".repeat(50)
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        val ids = chunks.map { it.id }.toSet()
        ids.size shouldBe chunks.size // All IDs should be unique
    }

    private fun createStrategy(
        chunkSize: Int = 200,
        chunkOverlap: Int = 50,
        minChunkLengthToEmbed: Int = 10,
        maxNumChunks: Int = 10000,
        keepSeparators: Boolean = true
    ) = RecursiveCharacterStrategy(
        chunkSize = chunkSize,
        chunkOverlap = chunkOverlap,
        minChunkLengthToEmbed = minChunkLengthToEmbed,
        maxNumChunks = maxNumChunks,
        keepSeparators = keepSeparators
    )
}
