package com.okestro.okchat.prompt.controller

import com.okestro.okchat.prompt.model.Prompt
import com.okestro.okchat.prompt.service.PromptService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/prompts")
class PromptController(
    private val promptService: PromptService
) {

    data class CreatePromptRequest(
        val type: String,
        val content: String
    )

    data class UpdatePromptRequest(
        val content: String
    )

    data class PromptResponse(
        val id: Long?,
        val type: String,
        val version: Int,
        val content: String,
        val isActive: Boolean
    ) {
        companion object {
            fun from(prompt: Prompt) = PromptResponse(
                id = prompt.id,
                type = prompt.type,
                version = prompt.version,
                content = prompt.content,
                isActive = prompt.active
            )
        }
    }

    data class PromptContentResponse(
        val type: String,
        val version: Int?,
        val content: String
    )

    @GetMapping("/{type}")
    suspend fun getPrompt(
        @PathVariable type: String,
        @RequestParam(required = false) version: Int?
    ): PromptContentResponse {
        log.info { "Getting prompt: type=$type, version=$version" }
        val content = promptService.getPrompt(type, version)
            ?: throw PromptNotFoundException("Prompt not found: type=$type, version=$version")

        val actualVersion = version ?: promptService.getLatestVersion(type)

        return PromptContentResponse(
            type = type,
            version = actualVersion,
            content = content
        )
    }

    @GetMapping("/{type}/versions")
    suspend fun getAllVersions(@PathVariable type: String): List<PromptResponse> {
        log.info { "Getting all versions of prompt: type=$type" }
        return promptService.getAllVersions(type).map { PromptResponse.from(it) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createPrompt(@RequestBody request: CreatePromptRequest): PromptResponse {
        log.info { "Creating new prompt: type=${request.type}" }
        val prompt = promptService.createPrompt(request.type, request.content)
        return PromptResponse.from(prompt)
    }

    @PutMapping("/{type}")
    suspend fun updatePrompt(
        @PathVariable type: String,
        @RequestBody request: UpdatePromptRequest
    ): PromptResponse {
        log.info { "Updating prompt: type=$type" }
        val prompt = promptService.updateLatestPrompt(type, request.content)
        return PromptResponse.from(prompt)
    }

    @DeleteMapping("/{type}/versions/{version}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deactivatePrompt(
        @PathVariable type: String,
        @PathVariable version: Int
    ) {
        log.info { "Deactivating prompt: type=$type, version=$version" }
        promptService.deactivatePrompt(type, version)
    }

    data class ErrorResponse(
        val message: String,
        val type: String
    )

    @ExceptionHandler(PromptNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlePromptNotFound(e: PromptNotFoundException): ErrorResponse {
        return ErrorResponse(
            message = e.message ?: "Prompt not found",
            type = "PROMPT_NOT_FOUND"
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(e: IllegalArgumentException): ErrorResponse {
        return ErrorResponse(
            message = e.message ?: "Invalid request",
            type = "INVALID_REQUEST"
        )
    }
}

class PromptNotFoundException(message: String) : RuntimeException(message)
