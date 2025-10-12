package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.RevokePathPermissionUseCaseIn
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RevokePathPermissionUseCaseTest : BehaviorSpec({

    val documentPathPermissionRepository = mockk<DocumentPathPermissionRepository>()
    val useCase = RevokePathPermissionUseCase(documentPathPermissionRepository)

    afterEach {
        clearAllMocks()
    }

    given("삭제할 경로 목록이 주어졌을 때") {
        val userId = 1L
        val pathsToRevoke = listOf("문서 > 팀 A", "문서 > 팀 B")
        every { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, pathsToRevoke) } returns Unit

        `when`("경로 권한을 취소하면") {
            val input = RevokePathPermissionUseCaseIn(userId, pathsToRevoke)
            val result = useCase.execute(input)

            then("성공 결과와 함께 삭제가 호출된다") {
                result.success shouldBe true
                result.revokedCount shouldBe pathsToRevoke.size
                verify(exactly = 1) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, pathsToRevoke) }
            }
        }
    }

    given("삭제할 경로 목록이 비어있을 때") {
        val userId = 2L
        val emptyPaths = emptyList<String>()

        `when`("경로 권한을 취소하면") {
            val input = RevokePathPermissionUseCaseIn(userId, emptyPaths)
            val result = useCase.execute(input)

            then("성공 결과와 함께 0이 반환되고, 삭제는 호출되지 않는다") {
                result.success shouldBe true
                result.revokedCount shouldBe 0
                verify(exactly = 0) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(any(), any()) }
            }
        }
    }
})
