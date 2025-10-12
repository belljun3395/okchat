package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.DeactivateUserUseCaseIn
import com.okestro.okchat.user.application.dto.DeactivateUserUseCaseOut
import com.okestro.okchat.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * UseCase: Deactivate user (soft delete)
 */
@Service
class DeactivateUserUseCase(
    private val userRepository: UserRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: DeactivateUserUseCaseIn): DeactivateUserUseCaseOut =
        withContext(Dispatchers.IO) {
            val user = userRepository.findById(useCaseIn.userId).orElseThrow {
                IllegalArgumentException("User not found: id=${useCaseIn.userId}")
            }

            userRepository.save(user.copy(active = false))
            log.info { "User deactivated: id=${useCaseIn.userId}, email=${user.email}" }

            DeactivateUserUseCaseOut(success = true, userId = useCaseIn.userId)
        }
}
