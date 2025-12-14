package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailClient
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.docs.client.user.UserSummaryDto
import com.okestro.okchat.knowledge.application.dto.CreateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant

class CreateKnowledgeBaseUseCaseTest : BehaviorSpec({

    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()
    val objectMapper = mockk<ObjectMapper>(relaxed = true)
    val userClient = mockk<UserClient>()
    val knowledgeMemberClient = mockk<KnowledgeMemberClient>()
    val knowledgeBaseEmailClient = mockk<KnowledgeBaseEmailClient>()

    val useCase = CreateKnowledgeBaseUseCase(
        knowledgeBaseRepository,
        objectMapper,
        userClient,
        knowledgeMemberClient,
        knowledgeBaseEmailClient
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base creation is requested") {
        val callerEmail = "admin@example.com"
        val input = CreateKnowledgeBaseUseCaseIn(
            callerEmail = callerEmail,
            name = "Test KB",
            description = "Description",
            type = KnowledgeBaseType.CONFLUENCE,
            config = mapOf("someKey" to "someValue")
        )

        val adminUser = UserSummaryDto(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = "SYSTEM_ADMIN"
        )

        val savedKb = KnowledgeBase(
            id = 100L,
            name = input.name,
            description = input.description,
            type = input.type,
            config = input.config.toMutableMap(),
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        `when`("SYSTEM_ADMIN requests creation") {
            coEvery { userClient.getByEmail(callerEmail) } returns adminUser
            coEvery { knowledgeBaseEmailClient.replaceEmailProviders(100L, any()) } returns Unit
            coEvery { knowledgeMemberClient.addMember(100L, callerEmail, callerEmail, "ADMIN") } returns Unit
            coEvery { knowledgeBaseRepository.save(any()) } returns savedKb

            val result = useCase.execute(input)

            then("Knowledge Base is created and returned") {
                result shouldNotBe null
                result.id shouldBe 100L
                result.name shouldBe input.name

                coVerify(exactly = 1) { knowledgeBaseRepository.save(any()) }
                coVerify(exactly = 1) { knowledgeBaseEmailClient.replaceEmailProviders(100L, any()) }
                coVerify(exactly = 1) { knowledgeMemberClient.addMember(100L, callerEmail, callerEmail, "ADMIN") }
            }
        }

        `when`("Non-admin user (USER role) requests creation") {
            val normalUser = adminUser.copy(role = "USER")
            coEvery { userClient.getByEmail(callerEmail) } returns normalUser

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("Caller user is not found") {
            coEvery { userClient.getByEmail(callerEmail) } returns null

            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
