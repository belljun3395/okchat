package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.GetUserByIdUseCaseIn
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetUserByIdUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val useCase = GetUserByIdUseCase(userRepository)

    afterEach {
        clearAllMocks()
    }

    given("활성화된 사용자가 존재할 때") {
        val userId = 1L
        val activeUser = User(
            id = userId,
            email = "test@example.com",
            name = "Test User",
            active = true
        )

        every { userRepository.findByIdAndActive(userId, true) } returns activeUser

        `when`("ID로 사용자를 조회하면") {
            val result = useCase.execute(GetUserByIdUseCaseIn(userId = userId))

            then("활성화된 사용자 정보가 반환된다") {
                result.user.shouldNotBeNull()
                result.user?.id shouldBe userId
                result.user?.active shouldBe true
                verify(exactly = 1) { userRepository.findByIdAndActive(userId, true) }
            }
        }
    }

    given("비활성화된 사용자이거나 존재하지 않는 사용자일 때") {
        val userId = 999L

        every { userRepository.findByIdAndActive(userId, true) } returns null

        `when`("ID로 사용자를 조회하면") {
            val result = useCase.execute(GetUserByIdUseCaseIn(userId = userId))

            then("null이 반환된다") {
                result.user.shouldBeNull()
                verify(exactly = 1) { userRepository.findByIdAndActive(userId, true) }
            }
        }
    }
})

