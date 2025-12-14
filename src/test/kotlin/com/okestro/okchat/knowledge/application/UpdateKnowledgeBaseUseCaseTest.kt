package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.knowledge.application.dto.UpdateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

class UpdateKnowledgeBaseUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()
    val knowledgeBaseEmailRepository = mockk<KnowledgeBaseEmailRepository>()
    val objectMapper = mockk<ObjectMapper>(relaxed = true)

    val useCase = UpdateKnowledgeBaseUseCase(
        userRepository,
        knowledgeBaseRepository,
        knowledgeBaseUserRepository,
        knowledgeBaseEmailRepository,
        objectMapper
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

        val caller = User(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = UserRole.USER
        )

        val existingKb = KnowledgeBase(
            name = "Old Name",
            type = KnowledgeBaseType.CONFLUENCE,
            config = mutableMapOf(),
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ).apply {
            ReflectionTestUtils.setField(this, "id", kbId)
        }

        `when`("KB ADMIN requests update") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.of(existingKb)
            every { knowledgeBaseRepository.save(any()) } answers { firstArg() }
            every { knowledgeBaseEmailRepository.deleteByKnowledgeBaseId(kbId) } just runs

            val result = useCase.execute(input)

            then("Information is updated and saved") {
                result.name shouldBe "Updated Name"
                result.description shouldBe "Updated Desc"
                verify(exactly = 1) { knowledgeBaseRepository.save(any()) }
                verify(exactly = 1) { knowledgeBaseEmailRepository.deleteByKnowledgeBaseId(kbId) }
            }
        }

        `when`("User without permission requests update") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.MEMBER,
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
