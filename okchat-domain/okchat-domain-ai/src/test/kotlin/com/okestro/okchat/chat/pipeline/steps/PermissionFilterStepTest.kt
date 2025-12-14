package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.client.docs.DocsClient
import com.okestro.okchat.ai.model.SearchResult
import com.okestro.okchat.ai.service.classifier.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PermissionFilterStep 단위 테스트")
class PermissionFilterStepTest {

    private lateinit var docsClient: DocsClient
    private lateinit var step: PermissionFilterStep

    @BeforeEach
    fun setUp() {
        docsClient = mockk()
        step = PermissionFilterStep(docsClient)
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
            createSearchResult("doc1", "Allowed Doc", "/allowed/doc1"),
            createSearchResult("doc2", "Blocked Doc", "/blocked/doc2"),
            createSearchResult("doc3", "Allowed Doc 2", "/allowed/nested/doc3")
        )
        val context = createContextWithSearchResults(
            userEmail = "user@example.com",
            searchResults = results
        )

        val allowedPaths = listOf("/allowed")

        coEvery { docsClient.getAllowedPaths("user@example.com", null) } returns allowedPaths

        // when
        val result = step.execute(context)

        // then
        result.search?.results?.size shouldBe 2
        result.search?.results?.map { it.id } shouldBe listOf("doc1", "doc3")
        coVerify(exactly = 1) { docsClient.getAllowedPaths("user@example.com", null) }
    }

    @Test
    @DisplayName("execute - 사용자를 찾을 수 없으면 빈 결과 반환")
    fun `should return empty results when user not found or no permissions`() = runTest {
        // given
        val context = createContextWithSearchResults(userEmail = "unknown@example.com")

        coEvery { docsClient.getAllowedPaths("unknown@example.com", null) } returns emptyList()

        // when
        val result = step.execute(context)

        // then
        result.search?.results?.size shouldBe 0
    }

    private fun createContextWithSearchResults(
        userEmail: String?,
        searchResults: List<SearchResult> = listOf(createSearchResult("doc1", "test", "/test"))
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

    private fun createSearchResult(id: String, title: String, path: String): SearchResult {
        return SearchResult(
            id = id,
            title = title,
            content = "content",
            path = path,
            spaceKey = "TEST",
            knowledgeBaseId = 0L,
            score = 0.8
        )
    }
}
