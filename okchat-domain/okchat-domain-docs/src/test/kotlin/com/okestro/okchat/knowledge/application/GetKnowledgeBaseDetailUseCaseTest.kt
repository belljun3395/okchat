package com.okestro.okchat.knowledge.application

import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailClient
import com.okestro.okchat.docs.client.user.KnowledgeBaseMembershipDto
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.docs.client.user.UserSummaryDto
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseDetailUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.util.Optional

class GetKnowledgeBaseDetailUseCaseTest : BehaviorSpec({

    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()
    val userClient = mockk<UserClient>()
    val knowledgeMemberClient = mockk<KnowledgeMemberClient>()
    val knowledgeBaseEmailClient = mockk<KnowledgeBaseEmailClient>()

    val useCase = GetKnowledgeBaseDetailUseCase(
        knowledgeBaseRepository,
        userClient,
        knowledgeMemberClient,
        knowledgeBaseEmailClient
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base detail retrieval is requested") {
        val kbId = 100L
        val callerEmail = "user@example.com"
        val input = GetKnowledgeBaseDetailUseCaseIn(kbId, callerEmail)

        val caller = UserSummaryDto(
            id = 1L,
            email = callerEmail,
            name = "Test User",
            role = "USER"
        )

        val kb = KnowledgeBase(
            id = kbId,
            name = "Test KB",
            description = null,
            type = KnowledgeBaseType.CONFLUENCE,
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            config = emptyMap<String, Any>()
        )

        `when`("KB member (ADMIN) requests") {
            coEvery { userClient.getByEmail(callerEmail) } returns caller
            coEvery { knowledgeMemberClient.getMembership(kbId = kbId, userId = caller.id) } returns KnowledgeBaseMembershipDto(
                knowledgeBaseId = kbId,
                userId = caller.id,
                role = "ADMIN",
                approvedBy = null,
                createdAt = Instant.now()
            )
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.of(kb)
            coEvery { knowledgeBaseEmailClient.getEmailProviders(kbId) } returns emptyList()

            val result = useCase.execute(input)

            then("상세 정보가 반환된다") {
                result shouldNotBe null
                result.id shouldBe kbId
                result.name shouldBe "Test KB"
            }
        }

        `when`("SYSTEM_ADMIN requests") {
            val systemAdmin = caller.copy(role = "SYSTEM_ADMIN")
            coEvery { userClient.getByEmail(callerEmail) } returns systemAdmin
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.of(kb)
            coEvery { knowledgeBaseEmailClient.getEmailProviders(kbId) } returns emptyList()

            val result = useCase.execute(input)

            then("상세 정보가 반환된다") {
                result shouldNotBe null
                result.id shouldBe kbId
            }
        }

        `when`("User without permission requests (MEMBER role)") {
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

        `when`("KB does not exist") {
            coEvery { userClient.getByEmail(callerEmail) } returns caller.copy(role = "SYSTEM_ADMIN")
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.empty()

            then("NoSuchElementException is thrown") {
                shouldThrow<NoSuchElementException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
