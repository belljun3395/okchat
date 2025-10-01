package com.okestro.okchat.task.support

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * Entity mapping for TASK_EXECUTION table
 */
@Table("TASK_EXECUTION")
data class TaskExecutionEntity(
    @Id
    val taskExecutionId: Long?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val taskName: String?,
    val exitCode: Int,
    val exitMessage: String?,
    val errorMessage: String?,
    val lastUpdated: LocalDateTime?
)

/**
 * Entity mapping for TASK_EXECUTION_PARAMS table
 */
@Table("TASK_EXECUTION_PARAMS")
data class TaskExecutionParamEntity(
    @Id
    val taskExecutionId: Long?,
    val taskParam: String
)
