package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.GetAllActiveUsersUseCaseIn
import com.okestro.okchat.user.application.dto.GetAllActiveUsersUseCaseOut
import com.okestro.okchat.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * UseCase: Get all active users
 */
@Service
class GetAllActiveUsersUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(useCaseIn: GetAllActiveUsersUseCaseIn): GetAllActiveUsersUseCaseOut =
        withContext(Dispatchers.IO) {
            val activeUsers = userRepository.findAll().filter { it.active }
            log.debug { "Get all active users: count=${activeUsers.size}" }
            GetAllActiveUsersUseCaseOut(users = activeUsers)
        }
}
