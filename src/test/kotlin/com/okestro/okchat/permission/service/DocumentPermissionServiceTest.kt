package com.okestro.okchat.permission.service

import com.okestro.okchat.permission.application.FilterSearchResultsUseCase
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseIn
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseOut
import com.okestro.okchat.search.application.SearchAllByPathUseCase
import com.okestro.okchat.search.application.SearchAllPathsUseCase
import com.okestro.okchat.search.application.dto.SearchAllByPathUseCaseIn
import com.okestro.okchat.search.application.dto.SearchAllByPathUseCaseOut
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseIn
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseOut
import com.okestro.okchat.search.model.Document
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseOut
import com.okestro.okchat.user.model.entity.User
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DocumentPermissionService Unit Tests")
class DocumentPermissionServiceTest {

    private lateinit var filterSearchResultsUseCase: FilterSearchResultsUseCase
    private lateinit var findUserByEmailUseCase: FindUserByEmailUseCase
    private lateinit var searchAllPathsUseCase: SearchAllPathsUseCase
    private lateinit var searchAllByPathUseCase: SearchAllByPathUseCase
    private lateinit var documentPermissionService: DocumentPermissionService

    @BeforeEach
    fun setUp() {
        filterSearchResultsUseCase = mockk()
        findUserByEmailUseCase = mockk()
        searchAllPathsUseCase = mockk()
        searchAllByPathUseCase = mockk()
        documentPermissionService = DocumentPermissionService(
            filterSearchResultsUseCase,
            findUserByEmailUseCase,
            searchAllPathsUseCase,
            searchAllByPathUseCase
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
        coEvery { findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn("nonexistent@example.com")) } returns
            FindUserByEmailUseCaseOut(user = null)

        // when
        val result = documentPermissionService.filterByUserEmail(results, "nonexistent@example.com")

        // then
        result.shouldBeEmpty()
        coVerify(exactly = 1) { findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn("nonexistent@example.com")) }
    }

    @Test
    @DisplayName("filterByUserEmail should return empty list when user is inactive")
    fun `should return empty list when user is inactive`() = runTest {
        // given
        val results = listOf(createSearchResult("팀회의"))
        coEvery { findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn("inactive@example.com")) } returns
            FindUserByEmailUseCaseOut(user = null)

        // when
        val result = documentPermissionService.filterByUserEmail(results, "inactive@example.com")

        // then
        result.shouldBeEmpty()
    }

    @Test
    @DisplayName("filterByUserEmail should filter results by user permissions")
    fun `should filter results by user permissions`() = runTest {
        // given
        val user = User(id = 1L, email = "user@example.com", name = "Test User")
        val results = listOf(
            createSearchResult("팀회의"),
            createSearchResult("프로젝트"),
            createSearchResult("업무일지")
        )
        val filteredResults = listOf(
            createSearchResult("팀회의"),
            createSearchResult("프로젝트")
        )

        coEvery { findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn("user@example.com")) } returns
            FindUserByEmailUseCaseOut(user = user)
        coEvery { filterSearchResultsUseCase.execute(FilterSearchResultsUseCaseIn(results, 1L)) } returns
            FilterSearchResultsUseCaseOut(filteredResults)

        // when
        val result = documentPermissionService.filterByUserEmail(results, "user@example.com")

        // then
        result shouldHaveSize 2
        result.map { it.path } shouldContainExactly listOf("팀회의", "프로젝트")
        coVerify(exactly = 1) { findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn("user@example.com")) }
        coVerify(exactly = 1) { filterSearchResultsUseCase.execute(FilterSearchResultsUseCaseIn(results, 1L)) }
    }

    @Test
    @DisplayName("searchAllPaths should delegate to SearchAllPathsUseCase")
    fun `should delegate searchAllPaths to SearchAllPathsUseCase`() {
        // given
        val paths = listOf("팀회의 > 2025", "프로젝트 > A", "업무일지")
        every { searchAllPathsUseCase.execute(any<SearchAllPathsUseCaseIn>()) } returns SearchAllPathsUseCaseOut(paths)

        // when
        val result = documentPermissionService.searchAllPaths()

        // then
        result shouldHaveSize 3
        result shouldContainExactly paths
        verify(exactly = 1) { searchAllPathsUseCase.execute(SearchAllPathsUseCaseIn()) }
    }

    @Test
    @DisplayName("searchAllByPath should delegate to SearchAllByPathUseCase")
    fun `should delegate searchAllByPath to SearchAllByPathUseCase`() = runTest {
        // given
        val path = "팀회의 > 2025"
        val documents = listOf(
            Document(
                id = "doc1",
                title = "회의록1",
                path = path
            ),
            Document(
                id = "doc2",
                title = "회의록2",
                path = path
            )
        )
        coEvery { searchAllByPathUseCase.execute(SearchAllByPathUseCaseIn(path)) } returns SearchAllByPathUseCaseOut(documents)

        // when
        val result = documentPermissionService.searchAllByPath(path)

        // then
        result shouldHaveSize 2
        result shouldContainExactly documents
    }

    private fun createSearchResult(path: String): SearchResult {
        return SearchResult(
            id = "id-${path.hashCode()}",
            title = "Title for $path",
            content = "Content for $path",
            path = path,
            spaceKey = "TEST",
            score = SearchScore.fromSimilarity(0.75)
        )
    }
}
