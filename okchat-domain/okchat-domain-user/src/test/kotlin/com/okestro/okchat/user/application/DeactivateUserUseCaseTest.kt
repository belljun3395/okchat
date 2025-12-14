package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.DeactivateUserUseCaseIn
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional

class DeactivateUserUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val useCase = DeactivateUserUseCase(userRepository)

    afterEach {
        clearAllMocks()
    }

    given("활성 사용자가 존재할 때") {
        val userId = 1L
        val activeUser = User(
            id = userId,
            email = "active@example.com",
            name = "Active User",
            active = true
        )
        val deactivatedUser = activeUser.copy(active = false)
        val savedUserSlot = slot<User>()

        every { userRepository.findById(userId) } returns Optional.of(activeUser)
        every { userRepository.save(capture(savedUserSlot)) } returns deactivatedUser

        `when`("사용자를 비활성화하면") {
            val result = useCase.execute(DeactivateUserUseCaseIn(userId))

            then("성공 결과가 반환되고 사용자가 비활성 상태로 저장된다") {
                result.success shouldBe true
                result.userId shouldBe userId
                verify(exactly = 1) { userRepository.save(any()) }
                savedUserSlot.captured.active shouldBe false
                savedUserSlot.captured.id shouldBe userId
            }
        }
    }

    given("이미 비활성화된 사용자일 때") {
        val userId = 2L
        val inactiveUser = User(
            id = userId,
            email = "inactive@example.com",
            name = "Inactive User",
            active = false
        )

        every { userRepository.findById(userId) } returns Optional.of(inactiveUser)
        every { userRepository.save(any()) } returns inactiveUser

        `when`("사용자를 비활성화하면") {
            val result = useCase.execute(DeactivateUserUseCaseIn(userId))

            then("성공 결과가 반환되고 save가 호출된다") {
                result.success shouldBe true
                result.userId shouldBe userId
                verify(exactly = 1) { userRepository.save(any()) }
            }
        }
    }

    given("존재하지 않는 사용자 ID일 때") {
        val userId = 999L

        every { userRepository.findById(userId) } returns Optional.empty()

        `when`("사용자를 비활성화하려고 시도하면") {
            val exception = shouldThrow<IllegalArgumentException> {
                useCase.execute(DeactivateUserUseCaseIn(userId))
            }

            then("IllegalArgumentException이 발생한다") {
                exception.message shouldContain "User not found"
                exception.message shouldContain userId.toString()
            }

            then("save가 호출되지 않는다") {
                verify(exactly = 0) { userRepository.save(any()) }
            }
        }
    }
})
