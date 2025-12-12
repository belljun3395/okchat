package com.okestro.okchat.user.application

import com.okestro.okchat.user.application.dto.FindOrCreateUserUseCaseIn
import com.okestro.okchat.user.application.dto.FindOrCreateUserUseCaseOut
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * UseCase: Find or create user by email
 * Useful for auto-registration when receiving email
 */
@Service
class FindOrCreateUserUseCase(
    private val userRepository: UserRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: FindOrCreateUserUseCaseIn): FindOrCreateUserUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val existingUser = userRepository.findByEmail(useCaseIn.email)

            if (existingUser != null) {
                log.debug { "User found: email=${useCaseIn.email}, id=${existingUser.id}" }
                return@withContext FindOrCreateUserUseCaseOut(user = existingUser, isNewUser = false)
            }

            // Create new user
            val newUser = User(
                email = useCaseIn.email,
                name = useCaseIn.name ?: useCaseIn.email.substringBefore("@")
            )

            val savedUser = userRepository.save(newUser)
            log.info { "New user created: email=${useCaseIn.email}, id=${savedUser.id}" }

            FindOrCreateUserUseCaseOut(user = savedUser, isNewUser = true)
        }
}
