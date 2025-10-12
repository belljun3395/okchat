package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.FindOrCreateUserUseCaseIn
import com.okestro.okchat.user.model.User
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FindOrCreateUserUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val useCase = FindOrCreateUserUseCase(userRepository)

    afterEach {
        clearAllMocks()
    }

    given("기존 사용자가 존재할 때") {
        val email = "existing@example.com"
        val existingUser = User(
            id = 1L,
            email = email,
            name = "Existing User",
            active = true
        )

        every { userRepository.findByEmail(email) } returns existingUser

        `when`("사용자를 찾거나 생성하면") {
            val result = useCase.execute(FindOrCreateUserUseCaseIn(email, "New Name"))

            then("기존 사용자가 반환된다") {
                result.user.id shouldBe 1L
                result.user.email shouldBe email
                result.user.name shouldBe "Existing User" // 기존 이름 유지
                result.isNewUser shouldBe false
            }

            then("save가 호출되지 않는다") {
                verify(exactly = 0) { userRepository.save(any()) }
            }
        }
    }

    given("사용자가 존재하지 않을 때 (이름 제공)") {
        val email = "new@example.com"
        val name = "New User"
        val newUser = User(
            id = null,
            email = email,
            name = name
        )
        val savedUser = newUser.copy(id = 2L)

        every { userRepository.findByEmail(email) } returns null
        every { userRepository.save(any<User>()) } returns savedUser

        `when`("사용자를 찾거나 생성하면") {
            val result = useCase.execute(FindOrCreateUserUseCaseIn(email, name))

            then("새로운 사용자가 생성되고 반환된다") {
                result.user.id shouldBe 2L
                result.user.email shouldBe email
                result.user.name shouldBe name
                result.isNewUser shouldBe true
                verify(exactly = 1) { userRepository.save(any()) }
            }
        }
    }

    given("사용자가 존재하지 않을 때 (이름 미제공)") {
        val email = "newuser@example.com"
        val savedUser = User(
            id = 3L,
            email = email,
            name = "newuser" // 이메일 prefix
        )

        every { userRepository.findByEmail(email) } returns null
        every { userRepository.save(any<User>()) } returns savedUser

        `when`("사용자를 찾거나 생성하면 (name = null)") {
            val result = useCase.execute(FindOrCreateUserUseCaseIn(email, null))

            then("이메일 prefix가 이름으로 설정된 사용자가 생성된다") {
                result.user.id.shouldNotBeNull()
                result.user.email shouldBe email
                result.user.name shouldBe "newuser"
                result.isNewUser shouldBe true
            }
        }
    }
})
