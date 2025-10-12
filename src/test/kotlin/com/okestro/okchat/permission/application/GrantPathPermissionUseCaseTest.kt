package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.GrantPathPermissionUseCaseIn
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.model.entity.DocumentPathPermission
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class GrantPathPermissionUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = GrantPathPermissionUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    given("사용자에게 아무런 권한이 없을 때") {
        val userId = 1L
        val path = "문서 > 팀 A"
        val permissionSlot = slot<DocumentPathPermission>()

        every { documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, path) } returns null
        every { documentPathPermissionRepository.findByUserId(userId) } returns emptyList()
        every { documentPathPermissionRepository.save(capture(permissionSlot)) } answers { firstArg() }

        `when`("새로운 경로에 READ 권한을 부여하면") {
            val input = GrantPathPermissionUseCaseIn(userId, path)
            useCase.execute(input)

            then("새로운 권한이 저장된다") {
                verify(exactly = 1) { documentPathPermissionRepository.save(any()) }
                permissionSlot.captured.userId shouldBe userId
                permissionSlot.captured.documentPath shouldBe path
                permissionSlot.captured.permissionLevel shouldBe PermissionLevel.READ
            }
            then("중복 제거 로직은 실행되지 않는다") {
                verify(exactly = 0) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(any(), any()) }
            }
        }
    }

    given("사용자에게 하위 경로 READ 권한과 DENY 권한이 이미 있을 때") {
        val userId = 2L
        val parentPath = "문서"
        val childReadPath = "문서 > 팀 A > 회의록"
        val childDenyPath = "문서 > 팀 B > 기밀"
        val permissionSlot = slot<DocumentPathPermission>()
        val deletedPathsSlot = slot<List<String>>()

        val existingPermissions = listOf(
            DocumentPathPermission(id = 2L, userId = userId, documentPath = childReadPath, spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null),
            DocumentPathPermission(id = 3L, userId = userId, documentPath = childDenyPath, spaceKey = "S", permissionLevel = PermissionLevel.DENY, grantedBy = null)
        )

        every { documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, parentPath) } returns null
        every { documentPathPermissionRepository.findByUserId(userId) } returns existingPermissions
        every { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, capture(deletedPathsSlot)) } returns Unit
        every { documentPathPermissionRepository.save(capture(permissionSlot)) } answers { firstArg() }

        `when`("상위 경로에 READ 권한을 부여하면") {
            val input = GrantPathPermissionUseCaseIn(userId, parentPath)
            useCase.execute(input)

            then("새로운 상위 경로 권한이 저장되고, 불필요한 하위 READ 권한은 삭제된다") {
                verify(exactly = 1) { documentPathPermissionRepository.save(any()) }
                verify(exactly = 1) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(any(), any()) }
                permissionSlot.captured.documentPath shouldBe parentPath
                deletedPathsSlot.captured shouldBe listOf(childReadPath)
            }
        }
    }

    given("이미 동일한 경로에 권한이 존재할 때") {
        val userId = 3L
        val path = "문서 > 팀 C"
        val existingPermission = DocumentPathPermission(id = 4L, userId = userId, documentPath = path, spaceKey = "S", permissionLevel = PermissionLevel.READ, grantedBy = null)
        every { documentPathPermissionRepository.findByUserIdAndDocumentPath(userId, path) } returns existingPermission

        `when`("권한을 다시 부여하면") {
            val input = GrantPathPermissionUseCaseIn(userId, path)
            val result = useCase.execute(input)

            then("기존 권한이 그대로 반환되고 아무 작업도 일어나지 않는다") {
                result.permission shouldBe existingPermission
                verify(exactly = 0) { documentPathPermissionRepository.save(any()) }
                verify(exactly = 0) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(any(), any()) }
            }
        }
    }
})
