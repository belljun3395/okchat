package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.config.SearchWeightConfig
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel

@DisplayName("ContentSearchStrategy Unit Tests")
class ContentSearchStrategyTest {

    private lateinit var searchClient: SearchClient
    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var weightConfig: SearchWeightConfig
    private lateinit var fieldConfig: SearchFieldWeightConfig
    private lateinit var strategy: ContentSearchStrategy

    @BeforeEach
    fun setUp() {
        searchClient = mockk()
        embeddingModel = mockk()
        weightConfig = createWeightConfig()
        fieldConfig = createFieldConfig()
        strategy = ContentSearchStrategy(searchClient, embeddingModel, weightConfig, fieldConfig)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("getName should return strategy name")
    fun `getName should return strategy name`() {
        // when
        val name = strategy.getName()

        // then
        name shouldBe "Content Hybrid Search"
    }

    @Test
    @DisplayName("strategy should be properly configured")
    fun `strategy should be properly configured`() {
        // when
        val name = strategy.getName()

        // then
        name shouldBe "Content Hybrid Search"
        // Protected methods are tested through actual search execution
    }

    private fun createWeightConfig(): SearchWeightConfig {
        val contentWeights = mockk<SearchWeightConfig.WeightSettings>()
        every { contentWeights.combine(any(), any()) } answers {
            val text = firstArg<Double>()
            val vector = secondArg<Double>()
            text * 0.3 + vector * 0.7
        }

        val config = mockk<SearchWeightConfig>()
        every { config.content } returns contentWeights
        return config
    }

    private fun createFieldConfig(): SearchFieldWeightConfig {
        val contentFields = mockk<SearchFieldWeightConfig.FieldWeights>()
        every { contentFields.queryByList() } returns listOf("content", "title", "path")
        every { contentFields.weightsList() } returns listOf(3, 2, 1)

        val config = mockk<SearchFieldWeightConfig>()
        every { config.content } returns contentFields
        return config
    }
}
