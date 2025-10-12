package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseIn
import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class FilterSearchResultsUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = FilterSearchResultsUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    fun createTestSearchResult(path: String): SearchResult = SearchResult(
        id = "id-$path",
        title = "제목",
        content = "내용",
        path = path,
        spaceKey = "TEST",
        score = SearchScore.PERFECT
    )

    given("사용자가 여러 문서에 대한 READ 및 DENY 권한을 가지고 있을 때") {
        val userId = 1L
        val permissions = listOf(
            DocumentPathPermission(id = 1L, userId = userId, documentPath = "문서 > 팀 A", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 2L, userId = userId, documentPath = "문서 > 팀 B", spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null),
            DocumentPathPermission(id = 3L, userId = userId, documentPath = "문서 > 팀 B > 개인", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null)
        )
        every { documentPathPermissionRepository.findByUserId(userId) } returns permissions

        val searchResults = listOf(
            createTestSearchResult("문서 > 팀 A > 회의록"),
            createTestSearchResult("문서 > 팀 C > 보고서"),
            createTestSearchResult("문서 > 팀 B > 공지"),
            createTestSearchResult("문서 > 팀 B > 개인 > 업무일지")
        )

        `when`("검색 결과를 필터링하면") {
            val input = FilterSearchResultsUseCaseIn(searchResults, userId)
            val result = useCase.execute(input)

            then("가장 구체적인 권한 규칙에 따라 접근 가능한 문서만 반환된다") {
                result.filteredResults shouldHaveSize 2
                result.filteredResults.map { it.path }.shouldContainExactly(
                    "문서 > 팀 A > 회의록",
                    "문서 > 팀 B > 개인 > 업무일지"
                )
            }
        }
    }

    given("사용자가 상위 경로에 READ, 하위 경로에 DENY 권한을 가질 때") {
        val userId = 2L
        val permissions = listOf(
            DocumentPathPermission(id = 4L, userId = userId, documentPath = "프로젝트", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 5L, userId = userId, documentPath = "프로젝트 > 기밀", spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )
        every { documentPathPermissionRepository.findByUserId(userId) } returns permissions

        val searchResults = listOf(
            createTestSearchResult("프로젝트 > 일반"),
            createTestSearchResult("프로젝트 > 기밀 > 1급"),
            createTestSearchResult("프로젝트 > 기밀")
        )

        `when`("검색 결과를 필터링하면") {
            val input = FilterSearchResultsUseCaseIn(searchResults, userId)
            val result = useCase.execute(input)

            then("더 구체적인 DENY 규칙이 우선하여 적용된다") {
                result.filteredResults.map { it.path }.shouldContainExactly("프로젝트 > 일반")
            }
        }
    }

    given("사용자가 아무런 권한도 가지고 있지 않을 때") {
        val userId = 3L
        every { documentPathPermissionRepository.findByUserId(userId) } returns emptyList()

        val searchResults = listOf(createTestSearchResult("문서1"), createTestSearchResult("문서2"))

        `when`("검색 결과를 필터링하면") {
            val input = FilterSearchResultsUseCaseIn(searchResults, userId)
            val result = useCase.execute(input)

            then("결과는 비어 있어야 한다") {
                result.filteredResults.shouldBeEmpty()
            }
        }
    }

    given("검색 결과가 비어있을 때") {
        val userId = 1L
        every { documentPathPermissionRepository.findByUserId(userId) } returns emptyList()

        `when`("빈 검색 결과를 필터링하면") {
            val input = FilterSearchResultsUseCaseIn(emptyList(), userId)
            val result = useCase.execute(input)

            then("결과는 비어 있어야 한다") {
                result.filteredResults.shouldBeEmpty()
            }
        }
    }
})
