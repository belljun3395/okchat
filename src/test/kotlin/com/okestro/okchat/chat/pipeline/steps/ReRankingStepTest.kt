package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.fixture.TestFixtures
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel

@DisplayName("ReRankingStep Unit Tests")
class ReRankingStepTest {

    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var step: ReRankingStep

    @BeforeEach
    fun setUp() {
        embeddingModel = mockk()
        step = ReRankingStep(embeddingModel)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("getStepName should return step name")
    fun `getStepName should return step name`() {
        // when
        val name = step.getStepName()

        // then
        name shouldBe "Re-Ranking"
    }

    @Test
    @DisplayName("shouldExecute should return true when deep think and has results")
    fun `shouldExecute should return true when deep think and has results`() {
        // given
        val searchResults = TestFixtures.searchResults(5)
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            analysis = createAnalysis(),
            search = ChatContext.Search(results = searchResults),
            isDeepThink = true
        )

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe true
    }

    @Test
    @DisplayName("shouldExecute should return false when not deep think")
    fun `shouldExecute should return false when not deep think`() {
        // given
        val searchResults = TestFixtures.searchResults(5)
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            analysis = createAnalysis(),
            search = ChatContext.Search(results = searchResults),
            isDeepThink = false
        )

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe false
    }

    @Test
    @DisplayName("shouldExecute should return false when no search results")
    fun `shouldExecute should return false when no search results`() {
        // given
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            analysis = createAnalysis(),
            search = null,
            isDeepThink = true
        )

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe false
    }

    @Test
    @DisplayName("execute should skip when only one result")
    fun `execute should skip when only one result`() = runTest {
        // given
        val searchResults = TestFixtures.searchResults(1)
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            analysis = createAnalysis(),
            search = ChatContext.Search(results = searchResults),
            isDeepThink = true
        )

        // when
        val result = step.execute(context)

        // then
        result.search?.results?.size shouldBe 1
    }

    @Test
    @DisplayName("execute should re-rank multiple results")
    fun `execute should re-rank multiple results`() = runTest {
        // given
        val searchResults = TestFixtures.searchResults(10) {
            maxScore = 0.9
            minScore = 0.5
        }
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test query"),
            analysis = createAnalysis(),
            search = ChatContext.Search(results = searchResults),
            isDeepThink = true
        )

        // Mock embedding responses
        val queryEmbedding = FloatArray(384) { 0.5f }
        val docEmbedding = FloatArray(384) { 0.6f }

        every { embeddingModel.embed(any<String>()) } returns queryEmbedding
        every { embeddingModel.embed(any<String>()) } returns docEmbedding

        // when
        val result = step.execute(context)

        // then
        result.search?.results?.size shouldBe searchResults.size
    }

    private fun createAnalysis(): ChatContext.Analysis {
        return ChatContext.Analysis(
            queryAnalysis = QueryClassifier.QueryAnalysis(
                type = QueryClassifier.QueryType.DOCUMENT_SEARCH,
                confidence = 0.9,
                keywords = emptyList()
            ),
            extractedTitles = emptyList(),
            extractedContents = emptyList(),
            extractedPaths = emptyList(),
            extractedKeywords = emptyList(),
            dateKeywords = emptyList()
        )
    }
}
