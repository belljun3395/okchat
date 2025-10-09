package com.okestro.okchat.task.support

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Spring Data JDBC Repository for Task Execution (Read-only access)
 * * NOTE: Spring Cloud Task manages the TASK_EXECUTION table internally using JDBC.
 * We use Spring Data JDBC (not JPA) to avoid conflicts:
 * - JPA's caching and entity lifecycle management would conflict with Spring Cloud Task's direct JDBC writes
 * - JDBC provides simple, cache-free access for querying task execution history
 * * This repository is primarily for monitoring and reporting purposes.
 */
@Repository
interface TaskExecutionRepository : CrudRepository<TaskExecutionEntity, Long> {

    /**
     * Find recent task executions ordered by start time
     */
    @Query(
        """
        SELECT * FROM TASK_EXECUTION
        ORDER BY START_TIME DESC
        LIMIT 50
        """
    )
    fun findRecentExecutions(): List<TaskExecutionEntity>

    /**
     * Get task execution statistics
     */
    @Query(
        """
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN EXIT_CODE = 0 THEN 1 ELSE 0 END) as success,
            SUM(CASE WHEN EXIT_CODE != 0 THEN 1 ELSE 0 END) as failure
        FROM TASK_EXECUTION
        """
    )
    fun getStatistics(): Map<String, Long>
}

/**
 * Repository for Task Execution Parameters
 * * NOTE: Read-only access to Spring Cloud Task's parameter table
 */
@Repository
interface TaskExecutionParamsRepository : CrudRepository<TaskExecutionParamEntity, Long> {

    /**
     * Find task parameters by execution ID
     */
    @Query(
        """
        SELECT TASK_PARAM
        FROM TASK_EXECUTION_PARAMS
        WHERE TASK_EXECUTION_ID = :executionId
        """
    )
    fun findParamsByExecutionId(@Param("executionId") executionId: Long): List<String>
}
