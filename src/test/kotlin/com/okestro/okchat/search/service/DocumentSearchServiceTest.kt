package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.ContentSearchResults
import com.okestro.okchat.search.model.KeywordSearchResults
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.PathSearchResults
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.model.TitleSearchResults
import com.okestro.okchat.search.strategy.MultiSearchStrategy
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient

@DisplayName("DocumentSearchService Unit Tests")
class DocumentSearchServiceTest {

    private lateinit var searchStrategy: MultiSearchStrategy
    private lateinit var openSearchClient: OpenSearchClient
    private lateinit var service: DocumentSearchService

    @BeforeEach
    fun setUp() {
        searchStrategy = mockk()
        openSearchClient = mockk()
        service = DocumentSearchService(searchStrategy, openSearchClient, "test-index")
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("multiSearch should delegate to search strategy")
    fun `multiSearch should delegate to search strategy`() = runTest {
        // given
        val titles = SearchTitles.fromStrings(listOf("Kotlin Guide"))
        val contents = SearchContents.fromStrings(listOf("programming"))
        val paths = SearchPaths.fromStrings(listOf("Development"))
        val keywords = SearchKeywords.fromStrings(listOf("kotlin", "tutorial"))

        val mockResult = MultiSearchResult.fromMap(
            mapOf(
                SearchType.KEYWORD to KeywordSearchResults(emptyList()),
                SearchType.TITLE to TitleSearchResults(emptyList()),
                SearchType.CONTENT to ContentSearchResults(emptyList()),
                SearchType.PATH to PathSearchResults(emptyList())
            )
        )

        every { searchStrategy.getStrategyName() } returns "HybridMultiSearch"
        coEvery { searchStrategy.search(any(), 50) } returns mockResult

        // when
        val result = service.multiSearch(
            titles = titles,
            contents = contents,
            paths = paths,
            keywords = keywords,
            topK = 50
        )

        // then
        result shouldBe mockResult
        coVerify(exactly = 1) { searchStrategy.search(any(), 50) }
    }

    @Test
    @DisplayName("multiSearch should handle null criteria")
    fun `multiSearch should handle null criteria`() = runTest {
        // given
        val keywords = SearchKeywords.fromStrings(listOf("test"))

        val mockResult = MultiSearchResult.fromMap(
            mapOf(
                SearchType.KEYWORD to KeywordSearchResults(emptyList()),
                SearchType.TITLE to TitleSearchResults(emptyList()),
                SearchType.CONTENT to ContentSearchResults(emptyList()),
                SearchType.PATH to PathSearchResults(emptyList())
            )
        )

        every { searchStrategy.getStrategyName() } returns "HybridMultiSearch"
        coEvery { searchStrategy.search(any(), 50) } returns mockResult

        // when
        val result = service.multiSearch(
            titles = null,
            contents = null,
            paths = null,
            keywords = keywords,
            topK = 50
        )

        // then
        result shouldBe mockResult
    }

    @Test
    @DisplayName("multiSearch should use custom topK value")
    fun `multiSearch should use custom topK value`() = runTest {
        // given
        val keywords = SearchKeywords.fromStrings(listOf("test"))

        val mockResult = MultiSearchResult.fromMap(
            mapOf(
                SearchType.KEYWORD to KeywordSearchResults(emptyList()),
                SearchType.TITLE to TitleSearchResults(emptyList()),
                SearchType.CONTENT to ContentSearchResults(emptyList()),
                SearchType.PATH to PathSearchResults(emptyList())
            )
        )

        every { searchStrategy.getStrategyName() } returns "HybridMultiSearch"
        coEvery { searchStrategy.search(any(), 100) } returns mockResult

        // when
        service.multiSearch(
            titles = null,
            contents = null,
            paths = null,
            keywords = keywords,
            topK = 100
        )

        // then
        coVerify(exactly = 1) { searchStrategy.search(any(), 100) }
    }
}
