package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GetPathPermissionsUseCaseIn
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

class GetPathPermissionsUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = GetPathPermissionsUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    given("Multiple permissions exist for a specific path") {
        val path = "Documents > Team A"
        val permissions = listOf(
            DocumentPathPermission(id = 1L, userId = 1L, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 2L, userId = 2L, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )
        every { documentPathPermissionRepository.findByDocumentPath(path) } returns permissions

        `when`("Permissions for the path are requested") {
            val input = GetPathPermissionsUseCaseIn(path)
            val result = useCase.execute(input)

            then("All permissions are returned") {
                result.permissions shouldHaveSize 2
                result.permissions shouldBe permissions
            }
        }
    }

    given("No permissions exist for a specific path") {
        val path = "Documents > Team B"
        every { documentPathPermissionRepository.findByDocumentPath(path) } returns emptyList()

        `when`("Permissions for the path are requested") {
            val input = GetPathPermissionsUseCaseIn(path)
            val result = useCase.execute(input)

            then("Empty list is returned") {
                result.permissions.shouldBeEmpty()
            }
        }
    }
})
