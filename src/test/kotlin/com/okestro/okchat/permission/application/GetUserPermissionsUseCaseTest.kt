package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GetUserPermissionsUseCaseIn
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.model.entity.DocumentPathPermission
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class GetUserPermissionsUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = GetUserPermissionsUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    given("사용자가 여러 권한을 가지고 있을 때") {
        val userId = 1L
        val permissions = listOf(
            DocumentPathPermission(id = 1L, userId = userId, documentPath = "path1", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 2L, userId = userId, documentPath = "path2", spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )
        every { documentPathPermissionRepository.findByUserId(userId) } returns permissions

        `when`("사용자의 권한을 조회하면") {
            val input = GetUserPermissionsUseCaseIn(userId)
            val result = useCase.execute(input)

            then("모든 권한 목록이 반환된다") {
                result.permissions shouldHaveSize 2
                result.permissions shouldBe permissions
            }
        }
    }

    given("사용자가 권한을 가지고 있지 않을 때") {
        val userId = 2L
        every { documentPathPermissionRepository.findByUserId(userId) } returns emptyList()

        `when`("사용자의 권한을 조회하면") {
            val input = GetUserPermissionsUseCaseIn(userId)
            val result = useCase.execute(input)

            then("빈 목록이 반환된다") {
                result.permissions.shouldBeEmpty()
            }
        }
    }
})
