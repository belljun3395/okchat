package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.GetUserByIdUseCaseIn
import com.okestro.okchat.user.application.dto.GetUserByIdUseCaseOut
import com.okestro.okchat.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * UseCase: Get active user by id
 * Returns null if user not found or inactive
 */
@Service
class GetUserByIdUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(useCaseIn: GetUserByIdUseCaseIn): GetUserByIdUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val user = userRepository.findByIdAndActive(useCaseIn.userId, true)
            log.debug { "Get user by id: id=${useCaseIn.userId}, found=${user != null}" }
            GetUserByIdUseCaseOut(user = user)
        }
}

