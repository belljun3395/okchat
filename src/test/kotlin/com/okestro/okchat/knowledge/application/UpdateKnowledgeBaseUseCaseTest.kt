package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailClient
import com.okestro.okchat.docs.client.user.KnowledgeBaseMembershipDto
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.docs.client.user.UserSummaryDto
import com.okestro.okchat.knowledge.application.dto.UpdateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Optional

class UpdateKnowledgeBaseUseCaseTest : BehaviorSpec({

    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()
    val objectMapper = mockk<ObjectMapper>(relaxed = true)
    val userClient = mockk<UserClient>()
    val knowledgeMemberClient = mockk<KnowledgeMemberClient>()
    val knowledgeBaseEmailClient = mockk<KnowledgeBaseEmailClient>()

    val useCase = UpdateKnowledgeBaseUseCase(
        knowledgeBaseRepository,
        objectMapper,
        userClient,
        knowledgeMemberClient,
        knowledgeBaseEmailClient
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base update is requested") {
        val kbId = 100L
        val callerEmail = "admin@example.com"
        val input = UpdateKnowledgeBaseUseCaseIn(
            kbId = kbId,
            callerEmail = callerEmail,
            name = "Updated Name",
            description = "Updated Desc",
            type = KnowledgeBaseType.CONFLUENCE,
            config = mapOf("key" to "value")
        )

        val caller = UserSummaryDto(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = "USER"
        )

        val existingKb = KnowledgeBase(
            id = kbId,
            name = "Old Name",
            description = null,
            type = KnowledgeBaseType.CONFLUENCE,
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            config = emptyMap<String, Any>()
        )

        `when`("KB ADMIN requests update") {
            coEvery { userClient.getByEmail(callerEmail) } returns caller
            coEvery { knowledgeMemberClient.getMembership(kbId = kbId, userId = caller.id) } returns KnowledgeBaseMembershipDto(
                knowledgeBaseId = kbId,
                userId = caller.id,
                role = "ADMIN",
                approvedBy = null,
                createdAt = Instant.now()
            )
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.of(existingKb)
            every { knowledgeBaseRepository.save(any()) } answers { firstArg() }
            coEvery { knowledgeBaseEmailClient.replaceEmailProviders(kbId, any()) } returns Unit

            val result = useCase.execute(input)

            then("Information is updated and saved") {
                result.name shouldBe "Updated Name"
                result.description shouldBe "Updated Desc"
                verify(exactly = 1) { knowledgeBaseRepository.save(any()) }
                coVerify(exactly = 1) { knowledgeBaseEmailClient.replaceEmailProviders(kbId, emptyList()) }
            }
        }

        `when`("User without permission requests update") {
            coEvery { userClient.getByEmail(callerEmail) } returns caller
            coEvery { knowledgeMemberClient.getMembership(kbId = kbId, userId = caller.id) } returns KnowledgeBaseMembershipDto(
                knowledgeBaseId = kbId,
                userId = caller.id,
                role = "MEMBER",
                approvedBy = null,
                createdAt = Instant.now()
            )

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
