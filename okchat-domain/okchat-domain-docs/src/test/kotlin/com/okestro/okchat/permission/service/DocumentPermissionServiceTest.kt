package com.okestro.okchat.permission.service

import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.docs.client.user.UserSummaryDto
import com.okestro.okchat.permission.application.FilterSearchResultsUseCase
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseIn
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseOut
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DocumentPermissionService Unit Tests")
class DocumentPermissionServiceTest {

    private lateinit var filterSearchResultsUseCase: FilterSearchResultsUseCase
    private lateinit var userClient: UserClient
    private lateinit var documentPermissionService: DocumentPermissionService

    @BeforeEach
    fun setUp() {
        filterSearchResultsUseCase = mockk()
        userClient = mockk()
        documentPermissionService = DocumentPermissionService(
            filterSearchResultsUseCase,
            userClient
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("filterByUserEmail should return empty list for empty results")
    fun `should return empty list for empty results`() = runTest {
        // when
        val result = documentPermissionService.filterByUserEmail(emptyList(), "test@example.com")

        // then
        result.shouldBeEmpty()
    }

    @Test
    @DisplayName("filterByUserEmail should return empty list when user not found")
    fun `should return empty list when user not found`() = runTest {
        // given
        val results = listOf(createSearchResult("팀회의"))
        coEvery { userClient.getByEmail("nonexistent@example.com") } returns null

        // when
        val result = documentPermissionService.filterByUserEmail(results, "nonexistent@example.com")

        // then
        result.shouldBeEmpty()
        coVerify(exactly = 1) { userClient.getByEmail("nonexistent@example.com") }
    }

    @Test
    @DisplayName("filterByUserEmail should return empty list when user is inactive")
    fun `should return empty list when user is inactive`() = runTest {
        // given
        val results = listOf(createSearchResult("팀회의"))
        coEvery { userClient.getByEmail("inactive@example.com") } returns null

        // when
        val result = documentPermissionService.filterByUserEmail(results, "inactive@example.com")

        // then
        result.shouldBeEmpty()
    }

    @Test
    @DisplayName("filterByUserEmail should filter results by user permissions")
    fun `should filter results by user permissions`() = runTest {
        // given
        val user = UserSummaryDto(id = 1L, email = "user@example.com", name = "Test User", role = "USER")
        val results = listOf(
            createSearchResult("팀회의"),
            createSearchResult("프로젝트"),
            createSearchResult("업무일지")
        )
        val filteredResults = listOf(
            createSearchResult("팀회의"),
            createSearchResult("프로젝트")
        )

        coEvery { userClient.getByEmail("user@example.com") } returns user
        coEvery { filterSearchResultsUseCase.execute(FilterSearchResultsUseCaseIn(results, 1L)) } returns
            FilterSearchResultsUseCaseOut(filteredResults)

        // when
        val result = documentPermissionService.filterByUserEmail(results, "user@example.com")

        // then
        result shouldHaveSize 2
        result.map { it.path } shouldContainExactly listOf("팀회의", "프로젝트")
        coVerify(exactly = 1) { userClient.getByEmail("user@example.com") }
        coVerify(exactly = 1) { filterSearchResultsUseCase.execute(FilterSearchResultsUseCaseIn(results, 1L)) }
    }

    private fun createSearchResult(path: String): SearchResult {
        return SearchResult(
            id = "id-${path.hashCode()}",
            title = "Title for $path",
            content = "Content for $path",
            path = path,
            spaceKey = "TEST",
            score = SearchScore.fromSimilarity(0.75),
            knowledgeBaseId = 0L
        )
    }
}
