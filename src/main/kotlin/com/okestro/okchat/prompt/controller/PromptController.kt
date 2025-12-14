package com.okestro.okchat.prompt.controller

import com.okestro.okchat.prompt.application.CreatePromptUseCase
import com.okestro.okchat.prompt.application.DeactivatePromptUseCase
import com.okestro.okchat.prompt.application.GetAllPromptTypesUseCase
import com.okestro.okchat.prompt.application.GetAllPromptVersionsUseCase
import com.okestro.okchat.prompt.application.GetLatestPromptVersionUseCase
import com.okestro.okchat.prompt.application.GetPromptUseCase
import com.okestro.okchat.prompt.application.UpdatePromptUseCase
import com.okestro.okchat.prompt.application.dto.CreatePromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.DeactivatePromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetAllPromptVersionsUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetLatestPromptVersionUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.UpdatePromptUseCaseIn
import com.okestro.okchat.prompt.model.entity.Prompt
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "Prompt API",
    description = "AI 프롬프트 관리 API. 프롬프트 버전 관리 및 CRUD 기능을 제공합니다."
)
class PromptController(
    private val getPromptUseCase: GetPromptUseCase,
    private val getLatestPromptVersionUseCase: GetLatestPromptVersionUseCase,
    private val getAllPromptVersionsUseCase: GetAllPromptVersionsUseCase,
    private val createPromptUseCase: CreatePromptUseCase,
    private val updatePromptUseCase: UpdatePromptUseCase,
    private val deactivatePromptUseCase: DeactivatePromptUseCase,
    private val getAllPromptTypesUseCase: GetAllPromptTypesUseCase
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
    @Operation(
        summary = "프롬프트 조회",
        description = "특정 타입의 프롬프트를 조회합니다. 버전을 지정하지 않으면 최신 버전을 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "프롬프트를 찾을 수 없음")
        ]
    )
    suspend fun getPrompt(
        @Parameter(description = "프롬프트 타입", example = "system", required = true)
        @PathVariable
        type: String,
        @Parameter(description = "프롬프트 버전 (선택사항)", example = "1", required = false)
        @RequestParam(required = false)
        version: Int?
    ): PromptContentResponse {
        log.info { "Getting prompt: type=$type, version=$version" }
        val content = getPromptUseCase.execute(GetPromptUseCaseIn(type, version)).content
            ?: throw PromptNotFoundException("Prompt not found: type=$type, version=$version")

        val actualVersion = version ?: getLatestPromptVersionUseCase.execute(GetLatestPromptVersionUseCaseIn(type)).version

        return PromptContentResponse(
            type = type,
            version = actualVersion,
            content = content
        )
    }

    @GetMapping("/types")
    @Operation(
        summary = "프롬프트 타입 목록 조회",
        description = "등록된 모든 프롬프트 타입을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    suspend fun getTypes(): List<String> {
        log.info { "Getting all prompt types" }
        return getAllPromptTypesUseCase.execute().types
    }

    @GetMapping("/{type}/versions")
    @Operation(
        summary = "프롬프트 버전 목록 조회",
        description = "특정 타입의 모든 프롬프트 버전을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    suspend fun getAllVersions(
        @Parameter(description = "프롬프트 타입", example = "system", required = true)
        @PathVariable
        type: String
    ): List<PromptResponse> {
        log.info { "Getting all versions of prompt: type=$type" }
        return getAllPromptVersionsUseCase.execute(GetAllPromptVersionsUseCaseIn(type)).prompts.map { PromptResponse.from(it) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "프롬프트 생성",
        description = "새로운 프롬프트를 생성합니다. 버전 1로 시작합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청")
        ]
    )
    suspend fun createPrompt(
        @Parameter(description = "프롬프트 생성 요청", required = true)
        @RequestBody
        request: CreatePromptRequest
    ): PromptResponse {
        log.info { "Creating new prompt: type=${request.type}" }
        val prompt = createPromptUseCase.execute(CreatePromptUseCaseIn(request.type, request.content)).prompt
        return PromptResponse.from(prompt)
    }

    @PutMapping("/{type}")
    @Operation(
        summary = "프롬프트 업데이트",
        description = "기존 프롬프트를 업데이트합니다. 새로운 버전이 생성됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "업데이트 성공"),
            ApiResponse(responseCode = "404", description = "프롬프트를 찾을 수 없음")
        ]
    )
    suspend fun updatePrompt(
        @Parameter(description = "프롬프트 타입", example = "system", required = true)
        @PathVariable
        type: String,
        @Parameter(description = "프롬프트 업데이트 요청", required = true)
        @RequestBody
        request: UpdatePromptRequest
    ): PromptResponse {
        log.info { "Updating prompt: type=$type" }
        val prompt = updatePromptUseCase.execute(UpdatePromptUseCaseIn(type, request.content)).prompt
        return PromptResponse.from(prompt)
    }

    @DeleteMapping("/{type}/versions/{version}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "프롬프트 비활성화",
        description = "특정 버전의 프롬프트를 비활성화합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "비활성화 성공"),
            ApiResponse(responseCode = "404", description = "프롬프트를 찾을 수 없음")
        ]
    )
    suspend fun deactivatePrompt(
        @Parameter(description = "프롬프트 타입", example = "system", required = true)
        @PathVariable
        type: String,
        @Parameter(description = "프롬프트 버전", example = "1", required = true)
        @PathVariable
        version: Int
    ) {
        log.info { "Deactivating prompt: type=$type, version=$version" }
        deactivatePromptUseCase.execute(DeactivatePromptUseCaseIn(type, version))
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
