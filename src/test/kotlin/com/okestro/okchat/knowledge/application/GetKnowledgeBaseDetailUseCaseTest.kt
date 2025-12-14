package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseDetailUseCaseIn
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
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

class GetKnowledgeBaseDetailUseCaseTest : BehaviorSpec({

    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()
    val knowledgeBaseEmailRepository = mockk<KnowledgeBaseEmailRepository>()
    val userRepository = mockk<UserRepository>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()

    val useCase = GetKnowledgeBaseDetailUseCase(
        knowledgeBaseRepository,
        knowledgeBaseEmailRepository,
        userRepository,
        knowledgeBaseUserRepository
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base detail retrieval is requested") {
        val kbId = 100L
        val callerEmail = "user@example.com"
        val input = GetKnowledgeBaseDetailUseCaseIn(kbId, callerEmail)

        val caller = User(
            id = 1L,
            email = callerEmail,
            name = "Test User",
            role = UserRole.USER
        )

        val kb = KnowledgeBase(
            name = "Test KB",
            type = KnowledgeBaseType.CONFLUENCE,
            config = mutableMapOf(),
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ).apply {
            ReflectionTestUtils.setField(this, "id", kbId)
        }

        `when`("KB member (ADMIN) requests") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.of(kb)
            every { knowledgeBaseEmailRepository.findAllByKnowledgeBaseId(kbId) } returns emptyList()

            val result = useCase.execute(input)

            then("상세 정보가 반환된다") {
                result shouldNotBe null
                result.id shouldBe kbId
                result.name shouldBe "Test KB"
            }
        }

        `when`("SYSTEM_ADMIN requests") {
            val systemAdmin = caller.copy(role = UserRole.SYSTEM_ADMIN)
            every { userRepository.findByEmail(callerEmail) } returns systemAdmin
            // System Admin doesn't need explicit membership check logic in canManageKb, but let's assume implementation
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.of(kb)
            every { knowledgeBaseEmailRepository.findAllByKnowledgeBaseId(kbId) } returns emptyList()

            val result = useCase.execute(input)

            then("상세 정보가 반환된다") {
                result shouldNotBe null
                result.id shouldBe kbId
            }
        }

        `when`("User without permission requests (MEMBER role)") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.MEMBER, // Not ADMIN
                createdAt = Instant.now()
            )

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("KB does not exist") {
            every { userRepository.findByEmail(callerEmail) } returns caller.copy(role = UserRole.SYSTEM_ADMIN)
            every { knowledgeBaseRepository.findById(kbId) } returns Optional.empty()

            then("NoSuchElementException is thrown") {
                shouldThrow<NoSuchElementException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
