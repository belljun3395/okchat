package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.config.SearchWeightConfig
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel

@DisplayName("KeywordSearchStrategy Unit Tests")
class KeywordSearchStrategyTest {

    private lateinit var searchClient: SearchClient
    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var weightConfig: SearchWeightConfig
    private lateinit var fieldConfig: SearchFieldWeightConfig
    private lateinit var strategy: KeywordSearchStrategy

    @BeforeEach
    fun setUp() {
        searchClient = mockk()
        embeddingModel = mockk()
        weightConfig = mockk()
        fieldConfig = mockk()
        strategy = KeywordSearchStrategy(searchClient, embeddingModel, weightConfig, fieldConfig)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("getName should return strategy name")
    fun `should return strategy name`() {
        // when
        val name = strategy.getName()

        // then
        name shouldBe "Keyword Hybrid Search"
    }
}
