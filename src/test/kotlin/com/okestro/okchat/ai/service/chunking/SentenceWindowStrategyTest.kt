package com.okestro.okchat.ai.service.chunking

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.ai.document.Document

@DisplayName("SentenceWindowStrategy Tests")
class SentenceWindowStrategyTest {

    @Test
    @DisplayName("should return strategy name")
    fun `should return strategy name`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 2)

        // when
        val name = strategy.getName()

        // then
        name shouldBe "Sentence Window Retrieval"
    }

    @Test
    @DisplayName("should create one chunk per sentence")
    fun `should create one chunk per sentence`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val text = "First sentence. Second sentence. Third sentence."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 3
        chunks[0].text shouldBe "First sentence"
        chunks[1].text shouldBe "Second sentence"
        chunks[2].text shouldBe "Third sentence"
    }

    @Test
    @DisplayName("should include window context in metadata")
    fun `should include window context in metadata`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val text = "First sentence. Second sentence. Third sentence."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        val middleChunk = chunks[1]
        middleChunk.metadata shouldContainKey "windowContext"
        val windowContext = middleChunk.metadata["windowContext"] as String
        windowContext shouldContain "First sentence"
        windowContext shouldContain "Second sentence"
        windowContext shouldContain "Third sentence"
    }

    @ParameterizedTest(name = "windowSize={0} should include {1} sentences in context")
    @CsvSource(
        "0, 1",
        "1, 3",
        "2, 5"
    )
    @DisplayName("should respect window size")
    fun `should respect window size`(windowSize: Int, expectedSentences: Int) {
        // given
        val strategy = SentenceWindowStrategy(windowSize = windowSize)
        val text = "S1. S2. S3. S4. S5. S6. S7."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        val middleChunk = chunks[3] // 4th sentence (0-indexed)
        val windowContext = middleChunk.metadata["windowContext"] as String
        val sentenceCount = windowContext.split(". ").filter { it.isNotBlank() }.size
        sentenceCount shouldBe expectedSentences
    }

    @Test
    @DisplayName("should handle edge sentences with smaller windows")
    fun `should handle edge sentences with smaller windows`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 2)
        val text = "First. Second. Third."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 3
        // First sentence window
        val firstWindow = chunks[0].metadata["windowContext"] as String
        firstWindow shouldContain "First"
        firstWindow shouldContain "Second"
        firstWindow shouldContain "Third"

        // Last sentence window
        val lastWindow = chunks[2].metadata["windowContext"] as String
        lastWindow shouldContain "First"
        lastWindow shouldContain "Second"
        lastWindow shouldContain "Third"
    }

    @Test
    @DisplayName("should store sentence index in metadata")
    fun `should store sentence index in metadata`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val text = "First. Second. Third."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.forEachIndexed { index, chunk ->
            chunk.metadata["sentenceIndex"] shouldBe index
            chunk.metadata["totalSentences"] shouldBe 3
        }
    }

    @Test
    @DisplayName("should include chunking strategy in metadata")
    fun `should include chunking strategy in metadata`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val text = "Test sentence."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks.forEach { chunk ->
            chunk.metadata["chunkingStrategy"] shouldBe "sentence_window"
        }
    }

    @Test
    @DisplayName("should handle single sentence document")
    fun `should handle single sentence document`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 2)
        val text = "Only one sentence."
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
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val document = Document("doc1", "", mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 1
        chunks[0].id shouldBe "doc1"
    }

    @Test
    @DisplayName("should handle multiple punctuation marks")
    fun `should handle multiple punctuation marks`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val text = "Question? Exclamation! Statement."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        chunks shouldHaveSize 3
        chunks[0].text shouldBe "Question"
        chunks[1].text shouldBe "Exclamation"
        chunks[2].text shouldBe "Statement"
    }

    @Test
    @DisplayName("should preserve original document metadata")
    fun `should preserve original document metadata`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
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
    @DisplayName("should generate unique chunk IDs")
    fun `should generate unique chunk IDs`() {
        // given
        val strategy = SentenceWindowStrategy(windowSize = 1)
        val text = "First. Second. Third. Fourth."
        val document = Document("doc1", text, mutableMapOf())

        // when
        val chunks = strategy.chunk(document)

        // then
        val ids = chunks.map { it.id }.toSet()
        ids.size shouldBe chunks.size
        chunks.forEachIndexed { index, chunk ->
            chunk.id shouldBe "doc1_sentence_$index"
        }
    }
}
