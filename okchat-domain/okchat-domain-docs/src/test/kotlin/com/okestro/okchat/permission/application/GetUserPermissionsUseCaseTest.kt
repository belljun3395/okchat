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

    given("User has multiple permissions") {
        val userId = 1L
        val permissions = listOf(
            DocumentPathPermission(id = 1L, userId = userId, documentPath = "path1", spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 2L, userId = userId, documentPath = "path2", spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )
        every { documentPathPermissionRepository.findByUserId(userId) } returns permissions

        `when`("User permissions are requested") {
            val input = GetUserPermissionsUseCaseIn(userId)
            val result = useCase.execute(input)

            then("All permissions are returned") {
                result.permissions shouldHaveSize 2
                result.permissions shouldBe permissions
            }
        }
    }

    given("User has no permissions") {
        val userId = 2L
        every { documentPathPermissionRepository.findByUserId(userId) } returns emptyList()

        `when`("User permissions are requested") {
            val input = GetUserPermissionsUseCaseIn(userId)
            val result = useCase.execute(input)

            then("Empty list is returned") {
                result.permissions.shouldBeEmpty()
            }
        }
    }
})
