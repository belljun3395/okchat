package com.okestro.okchat.user.controller

import com.okestro.okchat.user.application.DeactivateUserUseCase
import com.okestro.okchat.user.application.FindOrCreateUserUseCase
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.GetAllActiveUsersUseCase
import com.okestro.okchat.user.application.dto.DeactivateUserUseCaseIn
import com.okestro.okchat.user.application.dto.FindOrCreateUserUseCaseIn
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import com.okestro.okchat.user.application.dto.GetAllActiveUsersUseCaseIn
import com.okestro.okchat.user.model.User
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "User API",
    description = "사용자 관리 API. 사용자 생성, 조회, 비활성화 기능을 제공합니다."
)
class UserAdminWebController(
    private val getAllActiveUsersUseCase: GetAllActiveUsersUseCase,
    private val findUserByEmailUseCase: FindUserByEmailUseCase,
    private val findOrCreateUserUseCase: FindOrCreateUserUseCase,
    private val deactivateUserUseCase: DeactivateUserUseCase
) {

    /**
     * Get all active users
     */
    @GetMapping
    @Operation(
        summary = "활성 사용자 목록 조회",
        description = "모든 활성 상태인 사용자를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getAllUsers(): ResponseEntity<List<User>> {
        log.info { "Get all users request" }
        val output = getAllActiveUsersUseCase.execute(GetAllActiveUsersUseCaseIn())
        return ResponseEntity.ok(output.users)
    }

    /**
     * Get user by email
     */
    @GetMapping("/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<User> {
        log.info { "Get user by email: $email" }
        val output = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email))
        return output.user?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    /**
     * Create or update user
     */
    @PostMapping
    @Operation(
        summary = "사용자 생성 또는 업데이트",
        description = "새로운 사용자를 생성하거나 기존 사용자 정보를 업데이트합니다."
    )
    @ApiResponse(responseCode = "200", description = "생성/업데이트 성공")
    fun createUser(
        @Parameter(description = "사용자 생성 요청", required = true)
        @RequestBody
        request: CreateUserRequest
    ): ResponseEntity<User> {
        log.info { "Create user request: email=${request.email}, name=${request.name}" }
        val output = findOrCreateUserUseCase.execute(
            FindOrCreateUserUseCaseIn(email = request.email, name = request.name)
        )
        return ResponseEntity.ok(output.user)
    }

    /**
     * Deactivate user
     */
    @DeleteMapping("/{email}")
    fun deactivateUser(@PathVariable email: String): ResponseEntity<UserResponse> {
        log.info { "Deactivate user request: email=$email" }

        val findOutput = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email))
        val user = findOutput.user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(UserResponse(success = false, message = "User not found: $email"))

        deactivateUserUseCase.execute(DeactivateUserUseCaseIn(userId = user.id!!))

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
