package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.permission.service.DocumentPermissionService
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PermissionFilterStep 단위 테스트")
class PermissionFilterStepTest {

    private lateinit var documentPermissionService: DocumentPermissionService
    private lateinit var step: PermissionFilterStep

    @BeforeEach
    fun setUp() {
        documentPermissionService = mockk()
        step = PermissionFilterStep(documentPermissionService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("getStepName - 스텝 이름 반환")
    fun `should return step name`() {
        // when
        val name = step.getStepName()

        // then
        name shouldBe "Permission Filter"
    }

    @Test
    @DisplayName("shouldExecute - 검색 결과와 사용자 이메일이 있으면 true")
    fun `should execute when search results and user email exist`() {
        // given
        val context = createContextWithSearchResults(userEmail = "user@example.com")

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe true
    }

    @Test
    @DisplayName("shouldExecute - 검색 결과가 없으면 false")
    fun `should not execute when no search results`() {
        // given
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test", userEmail = "user@example.com"),
            analysis = createAnalysis(),
            search = null,
            isDeepThink = false
        )

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe false
    }

    @Test
    @DisplayName("shouldExecute - 사용자 이메일이 없으면 false")
    fun `should not execute when no user email`() {
        // given
        val context = createContextWithSearchResults(userEmail = null)

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe false
    }

    @Test
    @DisplayName("execute - 권한에 따라 검색 결과 필터링")
    fun `should filter search results by permission`() = runTest {
        // given
        val results = listOf(
            createSearchResult("doc1", "허용된 문서"),
            createSearchResult("doc2", "차단된 문서"),
            createSearchResult("doc3", "또 다른 허용 문서")
        )
        val context = createContextWithSearchResults(
            userEmail = "user@example.com",
            searchResults = results
        )

        val filteredResults = listOf(
            createSearchResult("doc1", "허용된 문서"),
            createSearchResult("doc3", "또 다른 허용 문서")
        )

        every { documentPermissionService.filterByUserEmail(results, "user@example.com") } returns filteredResults

        // when
        val result = step.execute(context)

        // then
        result.search?.results?.size shouldBe 2
        verify(exactly = 1) { documentPermissionService.filterByUserEmail(results, "user@example.com") }
    }

    @Test
    @DisplayName("execute - 사용자를 찾을 수 없으면 빈 결과 반환")
    fun `should return empty results when user not found`() = runTest {
        // given
        val context = createContextWithSearchResults(userEmail = "unknown@example.com")

        every { documentPermissionService.filterByUserEmail(any(), "unknown@example.com") } returns emptyList()

        // when
        val result = step.execute(context)

        // then
        result.search?.results?.size shouldBe 0
    }

    private fun createContextWithSearchResults(
        userEmail: String?,
        searchResults: List<SearchResult> = listOf(createSearchResult("doc1", "test"))
    ): ChatContext {
        return ChatContext(
            input = ChatContext.UserInput(message = "test", userEmail = userEmail),
            analysis = createAnalysis(),
            search = ChatContext.Search(results = searchResults),
            isDeepThink = false
        )
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

    private fun createSearchResult(id: String, title: String): SearchResult {
        return SearchResult(
            id = id,
            title = title,
            content = "content",
            path = "path",
            spaceKey = "TEST",
            score = SearchScore.fromSimilarity(0.8)
        )
    }
}
