package com.okestro.okchat.permission.application

import com.okestro.okchat.docs.client.user.KnowledgeBaseMembershipDto
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.docs.client.user.UserSummaryDto
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseIn
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.model.entity.DocumentPathPermission
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class FilterSearchResultsUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val userClient = mockk<UserClient>()
    val knowledgeMemberClient = mockk<KnowledgeMemberClient>()
    val useCase = FilterSearchResultsUseCase(
        documentPathPermissionRepository,
        userClient,
        knowledgeMemberClient
    )

    afterEach {
        clearAllMocks()
    }

    fun createTestSearchResult(path: String, kbId: Long = 1L): SearchResult = SearchResult(
        id = "id-$path",
        title = "Title",
        content = "Content",
        path = path,
        spaceKey = "TEST",
        knowledgeBaseId = kbId,
        score = SearchScore.PERFECT
    )

    given("User has READ and DENY permissions for multiple documents") {
        val userId = 1L
        val user = UserSummaryDto(id = userId, email = "user@test.com", name = "User", role = "USER")
        val permissions = listOf(
            DocumentPathPermission(id = 1L, userId = userId, documentPath = "Documents > Team A", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 2L, userId = userId, documentPath = "Documents > Team B", spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null),
            DocumentPathPermission(id = 3L, userId = userId, documentPath = "Documents > Team B > Personal", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null)
        )
        val memberships = listOf(
            KnowledgeBaseMembershipDto(
                knowledgeBaseId = 1L,
                userId = userId,
                role = "MEMBER",
                approvedBy = null,
                createdAt = Instant.now()
            )
        )

        coEvery { userClient.getById(userId) } returns user
        coEvery { knowledgeMemberClient.getMembershipsByUserId(userId) } returns memberships
        every { documentPathPermissionRepository.findByUserId(userId) } returns permissions

        val searchResults = listOf(
            createTestSearchResult("Documents > Team A > Minutes"),
            createTestSearchResult("Documents > Team C > Report"),
            createTestSearchResult("Documents > Team B > Notice"),
            createTestSearchResult("Documents > Team B > Personal > Journal")
        )

        `when`("Filtering search results") {
            val input = FilterSearchResultsUseCaseIn(searchResults, userId)
            val result = useCase.execute(input)

            then("Only accessible documents are returned based on most specific permission rules") {
                result.filteredResults.map { it.path }.shouldContainExactly(
                    "Documents > Team A > Minutes",
                    "Documents > Team C > Report",
                    "Documents > Team B > Personal > Journal"
                )
            }
        }
    }

    given("User has READ on parent path and DENY on child path") {
        val userId = 2L
        val user = UserSummaryDto(id = userId, email = "user2@test.com", name = "User2", role = "USER")
        val permissions = listOf(
            DocumentPathPermission(id = 4L, userId = userId, documentPath = "Project", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 5L, userId = userId, documentPath = "Project > Confidential", spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )
        val memberships = listOf(
            KnowledgeBaseMembershipDto(
                knowledgeBaseId = 1L,
                userId = userId,
                role = "MEMBER",
                approvedBy = null,
                createdAt = Instant.now()
            )
        )

        coEvery { userClient.getById(userId) } returns user
        coEvery { knowledgeMemberClient.getMembershipsByUserId(userId) } returns memberships
        every { documentPathPermissionRepository.findByUserId(userId) } returns permissions

        val searchResults = listOf(
            createTestSearchResult("Project > General"),
            createTestSearchResult("Project > Confidential > Top Secret"),
            createTestSearchResult("Project > Confidential")
        )

        `when`("Filtering search results") {
            val input = FilterSearchResultsUseCaseIn(searchResults, userId)
            val result = useCase.execute(input)

            then("More specific DENY rule takes precedence") {
                result.filteredResults.map { it.path }.shouldContainExactly("Project > General")
            }
        }
    }

    given("User has no permissions") {
        val userId = 3L
        val user = UserSummaryDto(id = userId, email = "user3@test.com", name = "User3", role = "USER")
        val memberships = listOf(
            KnowledgeBaseMembershipDto(
                knowledgeBaseId = 1L,
                userId = userId,
                role = "MEMBER",
                approvedBy = null,
                createdAt = Instant.now()
            )
        )

        coEvery { userClient.getById(userId) } returns user
        coEvery { knowledgeMemberClient.getMembershipsByUserId(userId) } returns memberships
        every { documentPathPermissionRepository.findByUserId(userId) } returns emptyList()

        val searchResults = listOf(createTestSearchResult("Doc1"), createTestSearchResult("Doc2"))

        `when`("Filtering search results") {
            val input = FilterSearchResultsUseCaseIn(searchResults, userId)
            val result = useCase.execute(input)

            then("Access is allowed by default if membership exists (no path permissions)") {
                result.filteredResults shouldHaveSize 2
            }
        }
    }

    given("Search results are empty") {
        val userId = 1L
        // No mocks needed for empty results check as it returns early

        `when`("Filtering empty search results") {
            val input = FilterSearchResultsUseCaseIn(emptyList(), userId)
            val result = useCase.execute(input)

            then("Result should be empty") {
                result.filteredResults.shouldBeEmpty()
            }
        }
    }
})
