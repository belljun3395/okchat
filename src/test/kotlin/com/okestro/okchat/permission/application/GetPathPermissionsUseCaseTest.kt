package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GetPathPermissionsUseCaseIn
import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
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

    given("특정 경로에 여러 권한이 존재할 때") {
        val path = "문서 > 팀 A"
        val permissions = listOf(
            DocumentPathPermission(id = 1L, userId = 1L, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 2L, userId = 2L, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )
        every { documentPathPermissionRepository.findByDocumentPath(path) } returns permissions

        `when`("경로의 권한을 조회하면") {
            val input = GetPathPermissionsUseCaseIn(path)
            val result = useCase.execute(input)

            then("모든 권한 목록이 반환된다") {
                result.permissions shouldHaveSize 2
                result.permissions shouldBe permissions
            }
        }
    }

    given("특정 경로에 권한이 없을 때") {
        val path = "문서 > 팀 B"
        every { documentPathPermissionRepository.findByDocumentPath(path) } returns emptyList()

        `when`("경로의 권한을 조회하면") {
            val input = GetPathPermissionsUseCaseIn(path)
            val result = useCase.execute(input)

            then("빈 목록이 반환된다") {
                result.permissions.shouldBeEmpty()
            }
        }
    }
})
