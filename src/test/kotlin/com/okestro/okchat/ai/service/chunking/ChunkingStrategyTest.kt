package com.okestro.okchat.ai.service.chunking

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document

@DisplayName("ChunkingStrategy Interface Tests")
class ChunkingStrategyTest {

    @Test
    @DisplayName("ChunkingStrategy should define required interface methods")
    fun `ChunkingStrategy should define required interface methods`() {
        // given
        val testStrategy = object : ChunkingStrategy {
            override fun chunk(document: Document): List<Document> {
                return listOf(document)
            }
            
            override fun getName(): String {
                return "Test Strategy"
            }
        }

        // when
        val name = testStrategy.getName()
        val doc = Document("id1", "test", mutableMapOf())
        val result = testStrategy.chunk(doc)

        // then
        name shouldBe "Test Strategy"
        result shouldBe listOf(doc)
    }

    @Test
    @DisplayName("ChunkingStrategyType enum should have expected values")
    fun `ChunkingStrategyType enum should have expected values`() {
        // when
        val types = ChunkingStrategyType.entries

        // then
        types.size shouldBe 3
        types shouldBe listOf(
            ChunkingStrategyType.RECURSIVE_CHARACTER,
            ChunkingStrategyType.SEMANTIC,
            ChunkingStrategyType.SENTENCE_WINDOW
        )
    }
}
