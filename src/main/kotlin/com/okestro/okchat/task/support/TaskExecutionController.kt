package com.okestro.okchat.task.support

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * REST API for querying Task execution history
 */
@RestController
@RequestMapping("/api/tasks")
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
    fun getAllTaskExecutions(): Flux<TaskExecutionDto> {
        return Flux.fromIterable(
            taskExecutionRepository.findRecentExecutions()
                .map { it.toDto() }
        )
    }

    /**
     * Get specific task execution by ID
     */
    @GetMapping("/{id}")
    fun getTaskExecution(@PathVariable id: Long): Mono<TaskExecutionDto> {
        return Mono.fromCallable {
            taskExecutionRepository.findById(id)
                .orElseThrow { NoSuchElementException("Task execution not found: $id") }
                .toDto()
        }
    }

    /**
     * Get task execution statistics
     */
    @GetMapping("/stats")
    fun getTaskStats(): Mono<TaskStatsDto> {
        return Mono.fromCallable {
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
    }

    /**
     * Get task execution parameters
     */
    @GetMapping("/{id}/params")
    fun getTaskParams(@PathVariable id: Long): Flux<String> {
        return Flux.fromIterable(
            taskExecutionParamsRepository.findParamsByExecutionId(id)
        )
    }
}
