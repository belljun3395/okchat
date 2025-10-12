package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.GetAllActiveUsersUseCaseIn
import com.okestro.okchat.user.model.User
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

    given("활성 사용자와 비활성 사용자가 섞여 있을 때") {
        val activeUser1 = User(id = 1L, email = "active1@example.com", name = "Active 1", active = true)
        val activeUser2 = User(id = 2L, email = "active2@example.com", name = "Active 2", active = true)
        val inactiveUser = User(id = 3L, email = "inactive@example.com", name = "Inactive", active = false)

        every { userRepository.findAll() } returns listOf(activeUser1, activeUser2, inactiveUser)

        `when`("모든 활성 사용자를 조회하면") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("활성 사용자만 반환된다") {
                result.users shouldHaveSize 2
                result.users.shouldContainExactly(activeUser1, activeUser2)
                result.users.all { it.active } shouldBe true
            }
        }
    }

    given("모든 사용자가 활성 상태일 때") {
        val activeUser1 = User(id = 1L, email = "user1@example.com", name = "User 1", active = true)
        val activeUser2 = User(id = 2L, email = "user2@example.com", name = "User 2", active = true)

        every { userRepository.findAll() } returns listOf(activeUser1, activeUser2)

        `when`("모든 활성 사용자를 조회하면") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("모든 사용자가 반환된다") {
                result.users shouldHaveSize 2
                result.users.shouldContainExactly(activeUser1, activeUser2)
            }
        }
    }

    given("활성 사용자가 없을 때") {
        val inactiveUser = User(id = 1L, email = "inactive@example.com", name = "Inactive", active = false)

        every { userRepository.findAll() } returns listOf(inactiveUser)

        `when`("모든 활성 사용자를 조회하면") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("빈 리스트가 반환된다") {
                result.users shouldHaveSize 0
            }
        }
    }

    given("사용자가 전혀 없을 때") {
        every { userRepository.findAll() } returns emptyList()

        `when`("모든 활성 사용자를 조회하면") {
            val result = useCase.execute(GetAllActiveUsersUseCaseIn())

            then("빈 리스트가 반환된다") {
                result.users shouldHaveSize 0
            }
        }
    }
})
