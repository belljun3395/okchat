package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.AddKnowledgeBaseMemberUseCaseIn
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
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class AddKnowledgeBaseMemberUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val knowledgeBaseUserRepository = mockk<KnowledgeBaseUserRepository>()

    val useCase = AddKnowledgeBaseMemberUseCase(
        userRepository,
        knowledgeBaseUserRepository
    )

    afterEach {
        clearAllMocks()
    }

    given("Knowledge Base member addition is requested") {
        val kbId = 100L
        val callerEmail = "admin@example.com"
        val targetEmail = "newmember@example.com"
        val input = AddKnowledgeBaseMemberUseCaseIn(
            kbId = kbId,
            callerEmail = callerEmail,
            targetEmail = targetEmail,
            role = KnowledgeBaseUserRole.MEMBER
        )

        val caller = User(
            id = 1L,
            email = callerEmail,
            name = "Admin",
            role = UserRole.USER
        )

        val targetUser = User(
            id = 2L,
            email = targetEmail,
            name = "New Member",
            role = UserRole.USER
        )

        `when`("KB ADMIN adds a new member") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { userRepository.findByEmail(targetEmail) } returns targetUser
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(targetUser.id!!, kbId) } returns null
            every { knowledgeBaseUserRepository.save(any()) } returns mockk()

            useCase.execute(input)

            then("Member is added") {
                verify(exactly = 1) { knowledgeBaseUserRepository.save(any()) }
            }
        }

        `when`("Attempting to add an existing member") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { userRepository.findByEmail(targetEmail) } returns targetUser
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(targetUser.id!!, kbId) } returns KnowledgeBaseUser(
                userId = targetUser.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.MEMBER,
                createdAt = Instant.now()
            )

            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("Target user is not found") {
            every { userRepository.findByEmail(callerEmail) } returns caller
            every { knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId) } returns KnowledgeBaseUser(
                userId = caller.id!!,
                knowledgeBaseId = kbId,
                role = KnowledgeBaseUserRole.ADMIN,
                createdAt = Instant.now()
            )
            every { userRepository.findByEmail(targetEmail) } returns null

            then("NoSuchElementException is thrown") {
                shouldThrow<NoSuchElementException> {
                    useCase.execute(input)
                }
            }
        }

        `when`("User without permission attempts to add a member") {
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
    }
})
