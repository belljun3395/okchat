package com.okestro.okchat.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Dynamic log level adjustment endpoint.
 *
 * Note: This maps under `/actuator/loggers` to align with Spring Boot conventions,
 * but is implemented as a regular controller (not an actuator endpoint) so that
 * it works even when actuator loggers endpoint is not exposed.
 */
@RestController
@RequestMapping("/actuator/loggers")
class LoggerController {

    private val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    @GetMapping
    fun getAllLoggers(): Map<String, String> {
        return loggerContext.loggerList.associate { logger ->
            logger.name to (logger.level?.toString() ?: "INHERITED")
        }
    }

    @GetMapping("/{loggerName}")
    fun getLogger(@PathVariable loggerName: String): LoggerInfo {
        val logger = loggerContext.getLogger(loggerName)
        return LoggerInfo(
            name = logger.name,
            configuredLevel = logger.level?.toString(),
            effectiveLevel = logger.effectiveLevel.toString()
        )
    }

    @PostMapping("/{loggerName}")
    fun setLogLevel(
        @PathVariable loggerName: String,
        @RequestBody request: LogLevelRequest
    ): ResponseEntity<String> {
        val logger = loggerContext.getLogger(loggerName)
        val oldLevel = logger.level

        logger.level = Level.toLevel(request.level)

        log.info { "Log level changed: $loggerName ${oldLevel ?: "INHERITED"} -> ${request.level} (reason=${request.reason})" }

        return ResponseEntity.ok("Logger '$loggerName' level set to ${request.level}")
    }

    data class LogLevelRequest(
        val level: String,
        val reason: String? = null
    )

    data class LoggerInfo(
        val name: String,
        val configuredLevel: String?,
        val effectiveLevel: String
    )
}
