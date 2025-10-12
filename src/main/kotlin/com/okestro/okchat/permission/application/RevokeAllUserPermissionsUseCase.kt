package com.okestro.okchat.permission.application

import com.okestro.okchat.permission.application.dto.RevokeAllUserPermissionsUseCaseIn
import com.okestro.okchat.permission.application.dto.RevokeAllUserPermissionsUseCaseOut
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class RevokeAllUserPermissionsUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository
) {
    @Transactional("transactionManager")
    fun execute(useCaseIn: RevokeAllUserPermissionsUseCaseIn): RevokeAllUserPermissionsUseCaseOut {
        val userId = useCaseIn.userId

        documentPathPermissionRepository.deleteByUserId(userId)
        log.info { "All path permissions revoked for user: user_id=$userId" }

        return RevokeAllUserPermissionsUseCaseOut(success = true, userId = userId)
    }
}
