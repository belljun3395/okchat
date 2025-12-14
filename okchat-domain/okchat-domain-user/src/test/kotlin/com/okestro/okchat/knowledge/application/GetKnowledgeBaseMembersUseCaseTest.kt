package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembersUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class GetKnowledgeBaseMembersUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()

    val useCase = GetKnowledgeBaseMembersUseCase(
        userRepository,
        knowledgeBaseUserRepository
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base member list retrieval is requested") {
        val kbId = 100L
        val callerEmail = "admin@example.com"
        val input = GetKnowledgeBaseMembersUseCaseIn(kbId, callerEmail)

        val caller = User(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = UserRole.USER
        )

        val member1 = User(
            id = 2L,
            email = "member1@example.com",
            name = "Member 1",
            role = UserRole.USER
        )

        val member2 = User(
            id = 3L,
            email = "member2@example.com",
            name = "Member 2",
            role = UserRole.USER
        )

        val callerId = caller.id!!
        val memberships = listOf(
            KnowledgeBaseUser(
                userId = callerId,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                approvedBy = 1L,
                createdAt = Instant.now()
            ),
            KnowledgeBaseUser(
                userId = member1.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.MEMBER,
                approvedBy = caller.id,
                createdAt = Instant.now()
            ),
            KnowledgeBaseUser(
                userId = member2.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.MEMBER,
                approvedBy = caller.id,
                createdAt = Instant.now()
            )
        )

        `when`("KB ADMIN requests member list") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(callerId, kbId) } returns memberships[0]
            every { knowledgeBaseUserRepository.findByKnowledgeBaseId(kbId) } returns memberships
            every { userRepository.findAllById(any<Iterable<Long>>()) } returns listOf(caller, member1, member2)

            val result = useCase.execute(input)

            then("All members are returned") {
                result shouldHaveSize 3
                result[0].userId shouldBe caller.id
                result[0].role shouldBe KnowledgeBaseUserRole.ADMIN
                result[1].userId shouldBe member1.id
                result[2].userId shouldBe member2.id
            }
        }

        `when`("SYSTEM_ADMIN requests member list") {
            val systemAdmin = caller.copy(role = UserRole.SYSTEM_ADMIN)
            every { userRepository.findByEmail(callerEmail) } returns systemAdmin
            every { knowledgeBaseUserRepository.findByKnowledgeBaseId(kbId) } returns memberships
            every { userRepository.findAllById(any<Iterable<Long>>()) } returns listOf(caller, member1, member2)

            val result = useCase.execute(input)

            then("All members are returned") {
                result shouldHaveSize 3
            }
        }

        `when`("User without permission requests member list") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(callerId, kbId) } returns null

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
