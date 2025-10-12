package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.RevokePathPermissionUseCaseIn
import com.okestro.okchat.permission.application.dto.RevokePathPermissionUseCaseOut
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class RevokePathPermissionUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    @Transactional("transactionManager")
    fun execute(useCaseIn: RevokePathPermissionUseCaseIn): RevokePathPermissionUseCaseOut {
        val (userId, documentPaths) = useCaseIn

        if (documentPaths.isEmpty()) {
            return RevokePathPermissionUseCaseOut(success = true, revokedCount = 0)
        }

        documentPathPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, documentPaths)
        log.info { "Bulk path permissions revoked: user_id=$userId, count=${documentPaths.size}" }

        return RevokePathPermissionUseCaseOut(success = true, revokedCount = documentPaths.size)
    }
}
