package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
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

class FindUserByEmailUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val useCase = FindUserByEmailUseCase(userRepository)

    afterEach {
        clearAllMocks()
    }

    given("활성화된 사용자가 존재할 때") {
        val email = "test@example.com"
        val activeUser = User(
            id = 1L,
            email = email,
            name = "Test User",
            active = true
        )

        every { userRepository.findByEmailAndActive(email, true) } returns activeUser

        `when`("이메일로 사용자를 조회하면") {
            val result = useCase.execute(FindUserByEmailUseCaseIn(email))

            then("활성화된 사용자 정보가 반환된다") {
                result.user.shouldNotBeNull()
                result.user?.email shouldBe email
                result.user?.id shouldBe 1L
                result.user?.active shouldBe true
                verify(exactly = 1) { userRepository.findByEmailAndActive(email, true) }
            }
        }
    }

    given("비활성화된 사용자만 존재할 때") {
        val email = "inactive@example.com"

        every { userRepository.findByEmailAndActive(email, true) } returns null

        `when`("이메일로 사용자를 조회하면") {
            val result = useCase.execute(FindUserByEmailUseCaseIn(email))

            then("null이 반환된다") {
                result.user.shouldBeNull()
            }
        }
    }

    given("존재하지 않는 사용자일 때") {
        val email = "nonexistent@example.com"

        every { userRepository.findByEmailAndActive(email, true) } returns null

        `when`("이메일로 사용자를 조회하면") {
            val result = useCase.execute(FindUserByEmailUseCaseIn(email))

            then("null이 반환된다") {
                result.user.shouldBeNull()
            }
        }
    }
})
