package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import java.time.Instant

private val log = KotlinLogging.logger {}

/**
 * Global exception handler for consistent error responses
 * Improves performance by avoiding stack trace generation for common errors
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(ex: WebExchangeBindException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }
        
        log.warn { "Validation error: $errors" }
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(
                timestamp = Instant.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Validation Failed",
                message = "Invalid request parameters",
                validationErrors = errors
            ))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations
            .associate { 
                it.propertyPath.toString() to it.message 
            }
        
        log.warn { "Constraint violation: $errors" }
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(
                timestamp = Instant.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Constraint Violation",
                message = "Request validation failed",
                validationErrors = errors
            ))
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException): ResponseEntity<ErrorResponse> {
        log.warn { "Bad request: ${ex.message}" }
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(
                timestamp = Instant.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = ex.reason ?: "Invalid request"
            ))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn { "Illegal argument: ${ex.message}" }
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(
                timestamp = Instant.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Invalid Argument",
                message = ex.message ?: "Invalid request parameter"
            ))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        log.warn { "Resource not found: ${ex.message}" }
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(
                timestamp = Instant.now(),
                status = HttpStatus.NOT_FOUND.value(),
                error = "Not Found",
                message = ex.message ?: "Resource not found"
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        // Log full stack trace only for unexpected errors
        log.error(ex) { "Unexpected error occurred" }
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse(
                timestamp = Instant.now(),
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal Server Error",
                message = "An unexpected error occurred",
                // Don't expose internal details in production
                details = if (isDebugMode()) ex.message else null
            ))
    }

    private fun isDebugMode(): Boolean {
        return System.getProperty("spring.profiles.active")?.contains("dev") ?: false
    }
}

/**
 * Standard error response structure
 */
data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val validationErrors: Map<String, String>? = null,
    val details: String? = null
)