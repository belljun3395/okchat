package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.KeywordSearchResults
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.strategy.MultiSearchStrategy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.opensearch.client.opensearch.OpenSearchClient

/**
 * Simple focused test for DocumentSearchService.
 * Tests the core delegation pattern without complex OpenSearch mocking.
 */
class DocumentSearchServiceSimpleTest : DescribeSpec({

    describe("DocumentSearchService - multiSearch delegation") {

        it("should delegate to configured search strategy") {
            // Given
            val mockStrategy = mockk<MultiSearchStrategy>()
            val mockOpenSearchClient = mockk<OpenSearchClient>()
            val indexName = "test-index"

            // Create expected result using proper types
            val expectedKeywordResults = KeywordSearchResults(
                results = listOf(
                    SearchResult.withSimilarity(
                        id = "doc1",
                        title = "Test Document",
                        content = "Test content",
                        path = "/test",
                        spaceKey = "TEST",
                        similarity = SearchScore.SimilarityScore(0.9)
                    )
                )
            )

            val expectedResult = MultiSearchResult.fromMap(
                mapOf(SearchType.KEYWORD to expectedKeywordResults)
            )

            coEvery { mockStrategy.search(any(), any()) } returns expectedResult

            every { mockStrategy.getStrategyName() } returns "TestStrategy"

            val service = DocumentSearchService(
                searchStrategy = mockStrategy,
                openSearchClient = mockOpenSearchClient,
                indexName = indexName
            )

            val keywords = SearchKeywords.fromStrings(listOf("test"))

            // When
            val result = runBlocking {
                service.multiSearch(
                    titles = null,
                    contents = null,
                    paths = null,
                    keywords = keywords,
                    topK = 50
                )
            }

            // Then
            result shouldBe expectedResult
            result.keywordResults.results.size shouldBe 1
            result.keywordResults.results.first().title shouldBe "Test Document"

            // Verify strategy was called
            coVerify(exactly = 1) {
                mockStrategy.search(
                    searchCriteria = match { criteria ->
                        criteria.size == 1 && criteria.first() is SearchKeywords
                    },
                    topK = 50
                )
            }
        }

        it("should handle multiple search criteria types") {
            // Given
            val mockStrategy = mockk<MultiSearchStrategy>()
            val mockOpenSearchClient = mockk<OpenSearchClient>()

            coEvery { mockStrategy.search(any(), any()) } returns MultiSearchResult.empty()

            every { mockStrategy.getStrategyName() } returns "TestStrategy"

            val service = DocumentSearchService(
                mockStrategy,
                mockOpenSearchClient,
                "test-index"
            )

            val titles = SearchTitles.fromStrings(listOf("title"))
            val contents = SearchContents.fromStrings(listOf("content"))
            val keywords = SearchKeywords.fromStrings(listOf("keyword"))

            // When
            runBlocking {
                service.multiSearch(
                    titles = titles,
                    contents = contents,
                    paths = null,
                    keywords = keywords,
                    topK = 50
                )
            }

            // Then - Verify 3 criteria were passed
            coVerify(exactly = 1) {
                mockStrategy.search(
                    searchCriteria = match { it.size == 3 },
                    topK = 50
                )
            }
        }

        it("should handle empty criteria by passing empty list") {
            // Given
            val mockStrategy = mockk<MultiSearchStrategy>()
            val mockOpenSearchClient = mockk<OpenSearchClient>()

            coEvery { mockStrategy.search(any(), any()) } returns MultiSearchResult.empty()

            every { mockStrategy.getStrategyName() } returns "TestStrategy"

            val service = DocumentSearchService(
                mockStrategy,
                mockOpenSearchClient,
                "test-index"
            )

            // When - all criteria null
            runBlocking {
                service.multiSearch(
                    titles = null,
                    contents = null,
                    paths = null,
                    keywords = null,
                    topK = 50
                )
            }

            // Then
            coVerify(exactly = 1) {
                mockStrategy.search(
                    searchCriteria = match { it.isEmpty() },
                    topK = 50
                )
            }
        }

        it("should use correct strategy name") {
            // Given
            val mockStrategy = mockk<MultiSearchStrategy>()
            val mockOpenSearchClient = mockk<OpenSearchClient>()

            coEvery { mockStrategy.search(any(), any()) } returns MultiSearchResult.empty()
            every { mockStrategy.getStrategyName() } returns "HybridSearchStrategy"

            val service = DocumentSearchService(
                mockStrategy,
                mockOpenSearchClient,
                "test-index"
            )

            // When
            runBlocking {
                service.multiSearch(null, null, null, null)
            }

            // Then
            coVerify(exactly = 1) { mockStrategy.getStrategyName() }
        }
    }
})
