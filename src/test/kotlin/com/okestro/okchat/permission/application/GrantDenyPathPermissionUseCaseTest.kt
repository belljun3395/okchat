package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GrantDenyPathPermissionUseCaseIn
import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class GrantDenyPathPermissionUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = GrantDenyPathPermissionUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    given("사용자에게 아무런 권한이 없을 때") {
        val userId = 1L
        val path = "문서 > 팀 A > 기밀"
        val permissionSlot = slot<DocumentPathPermission>()

        every { documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, path) } returns null
        every { documentPathPermissionRepository.save(capture(permissionSlot)) } answers { firstArg() }

        `when`("새로운 경로에 DENY 권한을 부여하면") {
            val input = GrantDenyPathPermissionUseCaseIn(userId, path)
            useCase.execute(input)

            then("새로운 DENY 권한이 저장된다") {
                verify(exactly = 1) { documentPathPermissionRepository.save(any()) }
                permissionSlot.captured.userId shouldBe userId
                permissionSlot.captured.documentPath shouldBe path
                permissionSlot.captured.permissionLevel shouldBe PermissionLevel.DENY
            }
        }
    }

    given("동일한 경로에 READ 권한이 이미 존재할 때") {
        val userId = 2L
        val path = "문서 > 팀 B"
        val existingReadPermission = DocumentPathPermission(id = 2L, userId = userId, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null)
        val permissionSlot = slot<DocumentPathPermission>()

        every { documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, path) } returns existingReadPermission
        every { documentPathPermissionRepository.delete(existingReadPermission) } returns Unit
        every { documentPathPermissionRepository.save(capture(permissionSlot)) } answers { firstArg() }

        `when`("DENY 권한을 부여하면") {
            val input = GrantDenyPathPermissionUseCaseIn(userId, path)
            useCase.execute(input)

            then("기존 READ 권한이 삭제되고 새로운 DENY 권한이 저장된다") {
                verify(exactly = 1) { documentPathPermissionRepository.delete(existingReadPermission) }
                verify(exactly = 1) { documentPathPermissionRepository.save(any()) }
                permissionSlot.captured.permissionLevel shouldBe PermissionLevel.DENY
            }
        }
    }

    given("이미 동일한 경로에 DENY 권한이 존재할 때") {
        val userId = 3L
        val path = "문서 > 팀 C"
        val existingDenyPermission = DocumentPathPermission(id = 3L, userId = userId, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        every { documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, path) } returns existingDenyPermission

        `when`("DENY 권한을 다시 부여하면") {
            val input = GrantDenyPathPermissionUseCaseIn(userId, path)
            val result = useCase.execute(input)

            then("기존 권한이 그대로 반환되고 아무 작업도 일어나지 않는다") {
                result.permission shouldBe existingDenyPermission
                verify(exactly = 0) { documentPathPermissionRepository.save(any()) }
                verify(exactly = 0) { documentPathPermissionRepository.delete(any()) }
            }
        }
    }
})
