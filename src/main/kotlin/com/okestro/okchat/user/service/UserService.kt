package com.okestro.okchat.user.service

import com.okestro.okchat.user.model.User
import com.okestro.okchat.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * Service for user management with caching support
 */
@Service
class UserService(
    private val userRepository: UserRepository
) {

    /**
     * Find user by email with caching
     * Returns null if user not found or inactive
     */
    @Cacheable(value = ["users"], key = "#email", unless = "#result == null")
    fun findByEmail(email: String): User? {
        log.debug { "Fetching user from database: email=$email" }
        return userRepository.findByEmailAndActive(email, true)
    }

    /**
     * Find or create user by email
     * Useful for auto-registration when receiving email
     */
    @Transactional("transactionManager")
    @CacheEvict(value = ["users"], key = "#email")
    fun findOrCreateUser(email: String, name: String? = null): User {
        val existingUser = userRepository.findByEmail(email)

        if (existingUser != null) {
            log.debug { "User found: email=$email, id=${existingUser.id}" }
            return existingUser
        }

        // Create new user
        val newUser = User(
            email = email,
            name = name ?: email.substringBefore("@")
        )

        return userRepository.save(newUser).also {
            log.info { "New user created: email=$email, id=${it.id}" }
        }
    }

    /**
     * Get all active users with caching
     */
    @Cacheable(value = ["users"], key = "'all-active'")
    fun getAllActiveUsers(): List<User> {
        log.debug { "Fetching all active users from database" }
        return userRepository.findAll().filter { it.active }
    }

    /**
     * Deactivate user (soft delete)
     */
    @Transactional("transactionManager")
    @CacheEvict(value = ["users"], allEntries = true)
    fun deactivateUser(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: id=$userId")
        }

        userRepository.save(user.copy(active = false))
        log.info { "User deactivated: id=$userId, email=${user.email}" }
    }
}
