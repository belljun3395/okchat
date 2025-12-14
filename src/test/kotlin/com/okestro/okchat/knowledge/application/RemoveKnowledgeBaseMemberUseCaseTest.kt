package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.RemoveKnowledgeBaseMemberUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.Instant

class RemoveKnowledgeBaseMemberUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()

    val useCase = RemoveKnowledgeBaseMemberUseCase(
        userRepository,
        knowledgeBaseUserRepository
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base member removal is requested") {
        val kbId = 100L
        val callerEmail = "admin@example.com"
        val targetUserId = 2L
        val input = RemoveKnowledgeBaseMemberUseCaseIn(
            kbId = kbId,
            callerEmail = callerEmail,
            targetUserId = targetUserId
        )

        val caller = User(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = UserRole.USER
        )

        val membership = KnowledgeBaseUser(
            userId = targetUserId,
            knowledgeBaseId = kbId,
            role = KnowledgeBaseUserRole.MEMBER,
            createdAt = Instant.now()
        )

        `when`("KB ADMIN requests member removal") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(targetUserId, kbId) } returns membership
            every { knowledgeBaseUserRepository.delete(membership) } just runs

            useCase.execute(input)

            then("Member is removed") {
                verify(exactly = 1) { knowledgeBaseUserRepository.delete(membership) }
            }
        }

        `when`("Target member does not exist") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(targetUserId, kbId) } returns null

            then("NoSuchElementException is thrown") {
                shouldThrow<NoSuchElementException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("User without permission attempts to remove a member") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns null

            then("IllegalAccessException is thrown") {
                shouldThrow<IllegalAccessException> {
                    useCase.execute(input)
                }
            }
        }
    }
})
