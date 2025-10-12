package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseOut
import com.okestro.okchat.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * UseCase: Find user by email
 * Returns null if user not found or inactive
 */
@Service
class FindUserByEmailUseCase(
    private val userRepository: UserRepository
) {
    fun execute(useCaseIn: FindUserByEmailUseCaseIn): FindUserByEmailUseCaseOut {
        val user = userRepository.findByEmailAndActive(useCaseIn.email, true)
        log.debug { "Find user by email: ${useCaseIn.email}, found=${user != null}" }
        return FindUserByEmailUseCaseOut(user = user)
    }
}
