package com.okestro.okchat.user.controller

import com.okestro.okchat.user.model.User
import com.okestro.okchat.user.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Admin API for user management
 *
 * Note: This is a basic CRUD API without authentication/authorization
 * In production, you should add proper admin authentication
 */
@RestController
@RequestMapping("/admin/users")
class UserAdminWebController(
    private val userService: UserService
) {

    /**
     * Get all active users
     */
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<User>> {
        log.info { "Get all users request" }
        val users = userService.getAllActiveUsers()
        return ResponseEntity.ok(users)
    }

    /**
     * Get user by email
     */
    @GetMapping("/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<User> {
        log.info { "Get user by email: $email" }
        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(user)
    }

    /**
     * Create or update user
     */
    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<User> {
        log.info { "Create user request: email=${request.email}, name=${request.name}" }
        val user = userService.findOrCreateUser(request.email, request.name)
        return ResponseEntity.ok(user)
    }

    /**
     * Deactivate user
     */
    @DeleteMapping("/{email}")
    fun deactivateUser(@PathVariable email: String): ResponseEntity<UserResponse> {
        log.info { "Deactivate user request: email=$email" }

        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(UserResponse(success = false, message = "User not found: $email"))

        userService.deactivateUser(user.id!!)

        return ResponseEntity.ok(
            UserResponse(success = true, message = "User deactivated: $email")
        )
    }
}

// Request/Response DTOs
data class CreateUserRequest(
    val email: String,
    val name: String
)

data class UserResponse(
    val success: Boolean,
    val message: String
)
