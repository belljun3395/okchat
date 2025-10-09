package com.okestro.okchat.task.support

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * Entity mapping for TASK_EXECUTION table
 * * NOTE: Spring Cloud Task manages this table using JDBC internally.
 * We use Spring Data JDBC (not JPA) for read-only access to avoid conflicts.
 */
@Table("TASK_EXECUTION")
data class TaskExecutionEntity(
    @Id
    @Column("TASK_EXECUTION_ID")
    val taskExecutionId: Long? = null,

    @Column("START_TIME")
    val startTime: LocalDateTime? = null,

    @Column("END_TIME")
    val endTime: LocalDateTime? = null,

    @Column("TASK_NAME")
    val taskName: String? = null,

    @Column("EXIT_CODE")
    val exitCode: Int = 0,

    @Column("EXIT_MESSAGE")
    val exitMessage: String? = null,

    @Column("ERROR_MESSAGE")
    val errorMessage: String? = null,

    @Column("LAST_UPDATED")
    val lastUpdated: LocalDateTime? = null
)

/**
 * Entity mapping for TASK_EXECUTION_PARAMS table
 * * NOTE: Managed by Spring Cloud Task internally
 */
@Table("TASK_EXECUTION_PARAMS")
data class TaskExecutionParamEntity(
    @Id
    @Column("TASK_EXECUTION_ID")
    val taskExecutionId: Long? = null,

    @Column("TASK_PARAM")
    val taskParam: String
)
