package com.okestro.okchat.permission.application

import com.okestro.okchat.docs.client.user.KnowledgeBaseMembershipDto
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.docs.client.user.UserSummaryDto
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseIn
import com.okestro.okchat.search.application.SearchAllPathsUseCase
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseIn
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseOut
import com.okestro.okchat.search.model.AllowedKnowledgeBases
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class GetAllowedPathsForUserUseCaseTest : BehaviorSpec({

    val userClient = mockk<UserClient>()
    val knowledgeMemberClient = mockk<KnowledgeMemberClient>()
    val searchAllPathsUseCase = mockk<SearchAllPathsUseCase>()
    val useCase = GetAllowedPathsForUserUseCase(
        userClient,
        knowledgeMemberClient,
        searchAllPathsUseCase
    )

    afterEach {
        clearAllMocks()
    }

    given("Request to get allowed paths for user") {
        val email = "user@example.com"
        val requestKbId = 1L
        val input = GetAllowedPathsForUserUseCaseIn(email, requestKbId)

        val user = UserSummaryDto(
            id = 1L,
            email = email,
            name = "User",
            role = "USER"
        )

        `when`("User is SYSTEM_ADMIN") {
            val systemAdmin = user.copy(role = "SYSTEM_ADMIN")
            coEvery { userClient.getByEmail(email) } returns systemAdmin
            every { searchAllPathsUseCase.execute(any()) } returns SearchAllPathsUseCaseOut(listOf("Path1"))

            val result = useCase.execute(input)

            then("Returns paths for requested KB") {
                result.paths shouldBe listOf("Path1")
                verify {
                    searchAllPathsUseCase.execute(
                        match {
                            it.allowedKbIds is AllowedKnowledgeBases.Subset &&
                                it.allowedKbIds.ids.contains(requestKbId)
                        }
                    )
                }
            }
        }

        `when`("User is KB ADMIN of requested KB") {
            coEvery { userClient.getByEmail(email) } returns user
            val userId = user.id
            val membership = KnowledgeBaseMembershipDto(
                knowledgeBaseId = requestKbId,
                userId = userId,
                role = "ADMIN",
                approvedBy = null,
                createdAt = Instant.now()
            )
            coEvery { knowledgeMemberClient.getMembershipsByUserId(userId) } returns listOf(membership)
            every { searchAllPathsUseCase.execute(any()) } returns SearchAllPathsUseCaseOut(listOf("Path1"))

            val result = useCase.execute(input)

            then("Returns paths") {
                result.paths shouldBe listOf("Path1")
                verify(exactly = 1) {
                    searchAllPathsUseCase.execute(
                        SearchAllPathsUseCaseIn(AllowedKnowledgeBases.Subset(listOf(requestKbId)))
                    )
                }
            }
        }

        `when`("User is NOT ADMIN of requested KB") {
            coEvery { userClient.getByEmail(email) } returns user
            val userId = user.id
            val membership = KnowledgeBaseMembershipDto(
                knowledgeBaseId = requestKbId,
                userId = userId,
                role = "MEMBER",
                approvedBy = null,
                createdAt = Instant.now()
            )
            coEvery { knowledgeMemberClient.getMembershipsByUserId(userId) } returns listOf(membership)

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
