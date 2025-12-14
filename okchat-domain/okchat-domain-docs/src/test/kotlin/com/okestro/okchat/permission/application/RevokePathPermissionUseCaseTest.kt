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

    given("A list of paths to revoke is provided") {
        val userId = 1L
        val pathsToRevoke = listOf("Document/Team A", "Document/Team B")
        every { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, pathsToRevoke) } returns Unit

        `when`("Revoking path permissions") {
            val input = RevokePathPermissionUseCaseIn(userId, pathsToRevoke)
            val result = useCase.execute(input)

            then("Revocation is successful and repository delete is called") {
                result.success shouldBe true
                result.revokedCount shouldBe pathsToRevoke.size
                verify(exactly = 1) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, pathsToRevoke) }
            }
        }
    }

    given("The list of paths to revoke is empty") {
        val userId = 2L
        val emptyPaths = emptyList<String>()

        `when`("Revoking path permissions") {
            val input = RevokePathPermissionUseCaseIn(userId, emptyPaths)
            val result = useCase.execute(input)

            then("Success is returned with count 0 and no repository delete call") {
                result.success shouldBe true
                result.revokedCount shouldBe 0
                verify(exactly = 0) { documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(any(), any()) }
            }
        }
    }
})
