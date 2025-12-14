package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.SearchCriteria
import com.okestro.okchat.search.model.SearchType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.ai.embedding.EmbeddingModel

class HybridMultiSearchStrategyTest : BehaviorSpec({

    val searchClient = mockk<SearchClient>()
    val embeddingModel = mockk<EmbeddingModel>()
    val fieldConfig = mockk<SearchFieldWeightConfig>()
    val strategy = HybridMultiSearchStrategy(searchClient, embeddingModel, fieldConfig)

    afterEach {
        clearAllMocks()
    }

    given("Multiple search criteria including Content type") {
        val keywordCriteria = mockk<SearchCriteria>()
        every { keywordCriteria.getSearchType() } returns SearchType.KEYWORD
        every { keywordCriteria.toQuery() } returns "keyword"
        every { keywordCriteria.isEmpty() } returns false
        every { keywordCriteria.size() } returns 1

        val contentCriteria = mockk<SearchCriteria>()
        every { contentCriteria.getSearchType() } returns SearchType.CONTENT
        every { contentCriteria.toQuery() } returns "content query"
        every { contentCriteria.isEmpty() } returns false
        every { contentCriteria.size() } returns 1

        val criteriaList = listOf(keywordCriteria, contentCriteria)
        val mockEmbedding = floatArrayOf(0.1f, 0.2f, 0.3f)

        // Mock field weights
        val keywordWeights = mockk<SearchFieldWeightConfig.FieldWeights>()
        every { keywordWeights.queryByList() } returns listOf("title")
        every { keywordWeights.weightsList() } returns listOf(1)

        val contentWeights = mockk<SearchFieldWeightConfig.FieldWeights>()
        every { contentWeights.queryByList() } returns listOf("content")
        every { contentWeights.weightsList() } returns listOf(1)

        every { SearchType.KEYWORD.getFieldWeights(fieldConfig) } returns keywordWeights
        every { SearchType.CONTENT.getFieldWeights(fieldConfig) } returns contentWeights

        // Mock embedding generation
        every { embeddingModel.embed("content query") } returns mockEmbedding

        // Mock search client
        val requestSlot = slot<List<HybridSearchRequest>>()
        coEvery { searchClient.multiHybridSearch(capture(requestSlot)) } returns listOf(
            mockk<HybridSearchResponse>(relaxed = true),
            mockk<HybridSearchResponse>(relaxed = true)
        )

        `when`("Search is executed") {
            strategy.search(criteriaList, topK = 5)

            then("Embedding should be generated from Content criteria") {
                requestSlot.captured.forEach { request ->
                    request.vectorQuery shouldBe mockEmbedding
                }
            }

            then("Should execute batched search request") {
                requestSlot.captured shouldHaveSize 2
                requestSlot.captured[0].textQuery shouldBe "keyword"
                requestSlot.captured[1].textQuery shouldBe "content query"
            }
        }
    }

    given("Search criteria without Content type") {
        val keywordCriteria = mockk<SearchCriteria>()
        every { keywordCriteria.getSearchType() } returns SearchType.KEYWORD
        every { keywordCriteria.toQuery() } returns "keyword only"
        every { keywordCriteria.isEmpty() } returns false
        every { keywordCriteria.size() } returns 1

        val criteriaList = listOf(keywordCriteria)

        val keywordWeights = mockk<SearchFieldWeightConfig.FieldWeights>()
        every { keywordWeights.queryByList() } returns listOf("title")
        every { keywordWeights.weightsList() } returns listOf(1)
        every { SearchType.KEYWORD.getFieldWeights(fieldConfig) } returns keywordWeights

        val requestSlot = slot<List<HybridSearchRequest>>()
        coEvery { searchClient.multiHybridSearch(capture(requestSlot)) } returns listOf(
            mockk<HybridSearchResponse>(relaxed = true)
        )

        `when`("Search is executed") {
            strategy.search(criteriaList, topK = 5)

            then("Empty embedding should be used (BM25 only)") {
                requestSlot.captured[0].vectorQuery shouldBe emptyList()
            }
        }
    }

    given("Empty criteria list") {
        `when`("Search is executed") {
            strategy.search(emptyList(), topK = 5)

            then("Should not call client") {
                coVerify(exactly = 0) { searchClient.multiHybridSearch(any()) }
            }
        }
    }
})
