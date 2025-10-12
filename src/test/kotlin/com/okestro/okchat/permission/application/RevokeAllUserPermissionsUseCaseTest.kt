package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.RevokeAllUserPermissionsUseCaseIn
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RevokeAllUserPermissionsUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = RevokeAllUserPermissionsUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    given("특정 사용자 ID가 주어졌을 때") {
        val userId = 123L
        every { documentPathPermissionRepository.deleteByUserId(userId) } returns Unit

        `when`("해당 사용자의 모든 권한을 취소하면") {
            val input = RevokeAllUserPermissionsUseCaseIn(userId)
            val result = useCase.execute(input)

            then("성공 결과와 함께 해당 사용자의 권한 삭제가 호출된다") {
                result.success shouldBe true
                result.userId shouldBe userId
                verify(exactly = 1) { documentPathPermissionRepository.deleteByUserId(userId) }
            }
        }
    }
})
