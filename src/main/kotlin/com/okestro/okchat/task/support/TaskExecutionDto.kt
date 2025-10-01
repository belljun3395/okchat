package com.okestro.okchat.task.support

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Duration
import java.time.LocalDateTime

/**
 * DTO for Task Execution
 */
data class TaskExecutionDto(
    val taskExecutionId: Long,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val startTime: LocalDateTime?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val endTime: LocalDateTime?,
    val taskName: String?,
    val exitCode: Int,
    val exitMessage: String?,
    val errorMessage: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val lastUpdated: LocalDateTime?
) {
    val durationSeconds: Long?
        get() = if (startTime != null && endTime != null) {
            Duration.between(startTime, endTime).seconds
        } else {
            null
        }

    val status: String
        get() = if (exitCode == 0) "SUCCESS" else "FAILURE"
}

/**
 * DTO for Task Execution Statistics
 */
data class TaskStatsDto(
    val total: Long,
    val success: Long,
    val failure: Long,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val lastExecution: LocalDateTime?
)

/**
 * Extension function to convert entity to DTO
 */
fun TaskExecutionEntity.toDto(): TaskExecutionDto {
    return TaskExecutionDto(
        taskExecutionId = this.taskExecutionId ?: 0L,
        startTime = this.startTime,
        endTime = this.endTime,
        taskName = this.taskName,
        exitCode = this.exitCode,
        exitMessage = this.exitMessage,
        errorMessage = this.errorMessage,
        lastUpdated = this.lastUpdated
    )
}
