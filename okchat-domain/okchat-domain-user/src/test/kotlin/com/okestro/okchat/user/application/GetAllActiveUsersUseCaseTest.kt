package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.GetAllActiveUsersUseCaseIn
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class GetAllActiveUsersUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val useCase = GetAllActiveUsersUseCase(userRepository)

    afterEach {
        clearAllMocks()
    }

    given("Mixed active and inactive users exist") {
        val activeUser1 = User(id = 1L, email = "active1@example.com", name = "Active 1", active = true)
        val activeUser2 = User(id = 2L, email = "active2@example.com", name = "Active 2", active = true)
        val inactiveUser = User(id = 3L, email = "inactive@example.com", name = "Inactive", active = false)

        every { userRepository.findAll() } returns listOf(activeUser1, activeUser2, inactiveUser)

        `when`("Retrieving all active users") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("Only active users are returned") {
                result.users shouldHaveSize 2
                result.users.shouldContainExactly(activeUser1, activeUser2)
                result.users.all { it.active } shouldBe true
            }
        }
    }

    given("All users are active") {
        val activeUser1 = User(id = 1L, email = "user1@example.com", name = "User 1", active = true)
        val activeUser2 = User(id = 2L, email = "user2@example.com", name = "User 2", active = true)

        every { userRepository.findAll() } returns listOf(activeUser1, activeUser2)

        `when`("Retrieving all active users") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("All users are returned") {
                result.users shouldHaveSize 2
                result.users.shouldContainExactly(activeUser1, activeUser2)
            }
        }
    }

    given("No active users exist") {
        val inactiveUser = User(id = 1L, email = "inactive@example.com", name = "Inactive", active = false)

        every { userRepository.findAll() } returns listOf(inactiveUser)

        `when`("Retrieving all active users") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("An empty list is returned") {
                result.users shouldHaveSize 0
            }
        }
    }

    given("No users exist at all") {
        every { userRepository.findAll() } returns emptyList()

        `when`("Retrieving all active users") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("An empty list is returned") {
                result.users shouldHaveSize 0
            }
        }
    }
})
