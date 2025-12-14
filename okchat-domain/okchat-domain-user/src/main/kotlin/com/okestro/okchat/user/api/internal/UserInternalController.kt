package com.okestro.okchat.user.api.internal

import com.okestro.okchat.user.api.internal.dto.InternalUserResponse
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.GetUserByIdUseCase
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import com.okestro.okchat.user.application.dto.GetUserByIdUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Internal API for user-domain access from other domains.
 *
 * NOTE: This API is intended for server-to-server calls only.
 */
@RestController
@RequestMapping("/internal/api/v1/users")
class UserInternalController(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val findUserByEmailUseCase: FindUserByEmailUseCase
) {

    @GetMapping("/{id}")
    suspend fun getById(
        @PathVariable id: Long
    ): ResponseEntity<InternalUserResponse> {
        log.debug { "[Internal] Get user by id: id=$id" }
        val output = getUserByIdUseCase.execute(GetUserByIdUseCaseIn(userId = id))
        val user = output.user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.ok(InternalUserResponse.from(user))
    }

    @GetMapping("/by-email")
    suspend fun getByEmail(
        @RequestParam email: String
    ): ResponseEntity<InternalUserResponse> {
        log.debug { "[Internal] Get user by email: email=$email" }
        val output = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email = email))
        val user = output.user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.ok(InternalUserResponse.from(user))
    }
}
