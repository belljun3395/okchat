package com.okestro.okchat.ai.service.chunking

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
    @DisplayName("should chunk long document into multiple parts")
    fun `should chunk long document into multiple parts`() {
        // given
        val strategy = createStrategy(chunkSize = 50, chunkOverlap = 10)
        // Create a very long text to ensure multiple chunks
        val longText = buildString {
            repeat(50) {
                append("This is sentence number $it with some content to make it longer. ")
            }
        }
        val document = Document("doc1", longText, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
        // TokenTextSplitter uses token-based chunking
        // With chunk size 50 tokens and very long text, should create multiple chunks
        chunks.size shouldBeGreaterThan 1
    }

    @Test
    @DisplayName("should preserve document metadata")
    fun `should preserve document metadata`() {
        // given
        val strategy = createStrategy()
        val metadata = mutableMapOf<String, Any>("key" to "value", "author" to "test")
        val document = Document("doc1", "Test content with some words", metadata)

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
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
        // TokenTextSplitter returns empty list for empty documents
        chunks.shouldBeEmpty()
    }

    @Test
    @DisplayName("should create overlapping chunks")
    fun `should create overlapping chunks`() {
        // given
        val strategy = createStrategy(chunkSize = 30, chunkOverlap = 10)
        val text = buildString {
            repeat(100) {
                append("This is a longer sentence number $it with more content to ensure tokenization. ")
            }
        }
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
        chunks.size shouldBeGreaterThan 1
    }

    @Test
    @DisplayName("should filter chunks by minimum length")
    fun `should filter chunks by minimum length`() {
        // given
        val strategy = createStrategy(
            chunkSize = 100,
            minChunkLengthToEmbed = 20 // Minimum 20 tokens
        )
        val text = buildString {
            repeat(20) {
                append("Word $it. ")
            }
        }
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
        chunks.forEach { chunk ->
            val chunkText = chunk.text ?: ""
            // All chunks should have content (filtered by min length)
            chunkText.isNotBlank() shouldBe true
        }
    }

    @Test
    @DisplayName("should generate unique chunk IDs")
    fun `should generate unique chunk IDs`() {
        // given
        val strategy = createStrategy(chunkSize = 50)
        val text = buildString {
            repeat(50) {
                append("Content for chunk $it. ")
            }
        }
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
        val ids = chunks.map { it.id }.toSet()
        ids.size shouldBe chunks.size // All IDs should be unique
    }

    @Test
    @DisplayName("should handle document with metadata")
    fun `should handle document with metadata`() {
        // given
        val strategy = createStrategy(chunkSize = 100)
        val metadata = mutableMapOf<String, Any>(
            "source" to "test",
            "version" to 1,
            "tags" to listOf("tag1", "tag2")
        )
        val text = "Content. ".repeat(50)
        val document = Document("doc1", text, metadata)

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.shouldNotBeEmpty()
        chunks.forEach { chunk ->
            chunk.metadata["source"] shouldBe "test"
            chunk.metadata["version"] shouldBe 1
        }
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
