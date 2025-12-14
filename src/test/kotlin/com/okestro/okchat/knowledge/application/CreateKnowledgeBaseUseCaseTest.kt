package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.knowledge.application.dto.CreateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class CreateKnowledgeBaseUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()
    val knowledgeBaseEmailRepository = mockk<KnowledgeBaseEmailRepository>()
    val objectMapper = mockk<ObjectMapper>(relaxed = true)

    val useCase = CreateKnowledgeBaseUseCase(
        userRepository,
        knowledgeBaseRepository,
        knowledgeBaseUserRepository,
        knowledgeBaseEmailRepository,
        objectMapper
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

        val adminUser = User(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = UserRole.SYSTEM_ADMIN
        )

        val savedKb = KnowledgeBase(
            name = input.name,
            description = input.description,
            type = input.type,
            config = input.config.toMutableMap(),
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ).apply {
            ReflectionTestUtils.setField(this, "id", 100L)
        }

        `when`("SYSTEM_ADMIN requests creation") {
            every { userRepository.findByEmail(callerEmail) } returns adminUser
            every { knowledgeBaseRepository.save(any()) } returns savedKb
            every { knowledgeBaseUserRepository.save(any()) } returns mockk<KnowledgeBaseUser>()

            val result = useCase.execute(input)

            then("Knowledge Base is created and returned") {
                result shouldNotBe null
                result.id shouldBe 100L
                result.name shouldBe input.name

                verify(exactly = 1) { knowledgeBaseRepository.save(any()) }
                verify(exactly = 1) { knowledgeBaseUserRepository.save(any()) }
            }
        }

        `when`("Non-admin user (USER role) requests creation") {
            val normalUser = adminUser.copy(role = UserRole.USER)
            every { userRepository.findByEmail(callerEmail) } returns normalUser

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("Caller user is not found") {
            every { userRepository.findByEmail(callerEmail) } returns null

            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
