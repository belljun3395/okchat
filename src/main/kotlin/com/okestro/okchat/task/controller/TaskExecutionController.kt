package com.okestro.okchat.task.controller

import com.okestro.okchat.task.dto.TaskExecutionDto
import com.okestro.okchat.task.dto.TaskStatsDto
import com.okestro.okchat.task.dto.toDto
import com.okestro.okchat.task.repository.TaskExecutionParamsRepository
import com.okestro.okchat.task.repository.TaskExecutionRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * REST API for querying Task execution history
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(
    name = "Task API",
    description = "백그라운드 작업 실행 이력 조회 API. Spring Cloud Task 실행 정보를 확인할 수 있습니다."
)
@ConditionalOnProperty(
    name = ["spring.cloud.task.initialize-enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TaskExecutionController(
    private val taskExecutionRepository: TaskExecutionRepository,
    private val taskExecutionParamsRepository: TaskExecutionParamsRepository
) {

    /**
     * Get all task executions (recent 50)
     */
    @GetMapping
    @Operation(
        summary = "작업 실행 이력 조회",
        description = "최근 50개의 작업 실행 이력을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getAllTaskExecutions(): Flux<TaskExecutionDto> =
        Mono.fromCallable {
            taskExecutionRepository.findRecentExecutions().map { it.toDto() }
        }
            .flatMapMany { Flux.fromIterable(it) }
            .subscribeOn(Schedulers.boundedElastic())

    /**
     * Get specific task execution by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "특정 작업 실행 정보 조회",
        description = "작업 ID를 통해 특정 작업 실행 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getTaskExecution(
        @Parameter(description = "작업 실행 ID", example = "1", required = true)
        @PathVariable
        id: Long
    ): Mono<TaskExecutionDto> =
        Mono.fromCallable {
            taskExecutionRepository.findById(id)
                .orElseThrow { NoSuchElementException("Task execution not found: $id") }
                .toDto()
        }
            .subscribeOn(Schedulers.boundedElastic())

    /**
     * Get task execution statistics
     */
    @GetMapping("/stats")
    @Operation(
        summary = "작업 실행 통계 조회",
        description = "작업 실행 통계 정보를 조회합니다 (총 개수, 성공/실패 개수, 마지막 실행 시간)."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getTaskStats(): Mono<TaskStatsDto> =
        Mono.fromCallable {
            val stats = taskExecutionRepository.getStatistics()
            val lastExecution = taskExecutionRepository.findRecentExecutions()
                .firstOrNull()
                ?.startTime

            TaskStatsDto(
                total = stats["total"] ?: 0L,
                success = stats["success"] ?: 0L,
                failure = stats["failure"] ?: 0L,
                lastExecution = lastExecution
            )
        }
            .subscribeOn(Schedulers.boundedElastic())

    /**
     * Get task execution parameters
     */
    @GetMapping("/{id}/params")
    fun getTaskParams(@PathVariable id: Long): Flux<String> =
        Mono.fromCallable {
            taskExecutionParamsRepository.findParamsByExecutionId(id)
        }
            .flatMapMany { Flux.fromIterable(it) }
            .subscribeOn(Schedulers.boundedElastic())
}
