package com.okestro.okchat.task.support

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * TODO: refactor to reactive repository
 * Spring Data JDBC Repository for Task Execution
 */
@Repository
interface TaskExecutionRepository : CrudRepository<TaskExecutionEntity, Long> {

    @Query(
        """
        SELECT * FROM TASK_EXECUTION
        ORDER BY START_TIME DESC
        LIMIT 50
        """
    )
    fun findRecentExecutions(): List<TaskExecutionEntity>

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
 */
@Repository
interface TaskExecutionParamsRepository : CrudRepository<TaskExecutionParamEntity, Long> {

    @Query(
        """
        SELECT TASK_PARAM
        FROM TASK_EXECUTION_PARAMS
        WHERE TASK_EXECUTION_ID = :executionId
        """
    )
    fun findParamsByExecutionId(executionId: Long): List<String>
}
