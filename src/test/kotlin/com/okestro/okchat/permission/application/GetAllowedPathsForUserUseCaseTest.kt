package com.okestro.okchat.permission.application

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.permission.application.dto.GetAllowedPathsForUserUseCaseIn
import com.okestro.okchat.search.application.SearchAllPathsUseCase
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseOut
import com.okestro.okchat.search.model.AllowedKnowledgeBases
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseOut
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.model.entity.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class GetAllowedPathsForUserUseCaseTest : BehaviorSpec({

    val findUserByEmailUseCase = mockk<FindUserByEmailUseCase>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()
    val searchAllPathsUseCase = mockk<SearchAllPathsUseCase>()
    val useCase = GetAllowedPathsForUserUseCase(
        findUserByEmailUseCase,
        knowledgeBaseUserRepository,
        searchAllPathsUseCase
    )

    afterEach {
        clearAllMocks()
    }

    given("Request to get allowed paths for user") {
        val email = "user@example.com"
        val requestKbId = 1L
        val input = GetAllowedPathsForUserUseCaseIn(email, requestKbId)

        val user = User(
            id = 1L,
            email = email,
            name = "User",
            role = UserRole.USER
        )

        `when`("User is SYSTEM_ADMIN") {
            val systemAdmin = user.copy(role = UserRole.SYSTEM_ADMIN)
            coEvery { findUserByEmailUseCase.execute(any()) } returns FindUserByEmailUseCaseOut(systemAdmin)
            coEvery { searchAllPathsUseCase.execute(any()) } returns SearchAllPathsUseCaseOut(listOf("Path1"))

            val result = useCase.execute(input)

            then("Returns paths for requested KB") {
                result.paths shouldBe listOf("Path1")
                coVerify {
                    searchAllPathsUseCase.execute(
                        match {
                            it.allowedKbIds is AllowedKnowledgeBases.Subset &&
                                (it.allowedKbIds as AllowedKnowledgeBases.Subset).ids.contains(requestKbId)
                        }
                    )
                }
            }
        }

        `when`("User is KB ADMIN of requested KB") {
            coEvery { findUserByEmailUseCase.execute(any()) } returns FindUserByEmailUseCaseOut(user)
            val userId = user.id!!
            val membership = KnowledgeBaseUser(
                userId = userId,
                knowledgeBaseId = requestKbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { knowledgeBaseUserRepository.findByUserId(userId) } returns listOf(membership)
            coEvery { searchAllPathsUseCase.execute(any()) } returns SearchAllPathsUseCaseOut(listOf("Path1"))

            val result = useCase.execute(input)

            then("Returns paths") {
                result.paths shouldBe listOf("Path1")
            }
        }

        `when`("User is NOT ADMIN of requested KB") {
            coEvery { findUserByEmailUseCase.execute(any()) } returns FindUserByEmailUseCaseOut(user)
            val userId = user.id!!
            val membership = KnowledgeBaseUser(
                userId = userId,
                knowledgeBaseId = requestKbId,
                role = KnowledgeBaseUserRole.MEMBER, // Not ADMIN
                createdAt = Instant.now()
            )
            every { knowledgeBaseUserRepository.findByUserId(userId) } returns listOf(membership)

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
