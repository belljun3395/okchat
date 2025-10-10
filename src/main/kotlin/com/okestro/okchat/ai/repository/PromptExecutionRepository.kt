package com.okestro.okchat.ai.repository

import com.okestro.okchat.ai.model.ExecutionStatus
import com.okestro.okchat.ai.model.PromptExecution
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PromptExecutionRepository : JpaRepository<PromptExecution, Long> {

    /**
     * Find all executions for a specific prompt type
     */
    fun findByPromptTypeOrderByCreatedAtDesc(
        promptType: String,
        pageable: Pageable
    ): Page<PromptExecution>

    /**
     * Find all executions for a specific prompt type and version
     */
    fun findByPromptTypeAndPromptVersionOrderByCreatedAtDesc(
        promptType: String,
        promptVersion: Int,
        pageable: Pageable
    ): Page<PromptExecution>

    /**
     * Find executions by session ID
     */
    fun findBySessionIdOrderByCreatedAtDesc(
        sessionId: String,
        pageable: Pageable
    ): Page<PromptExecution>

    /**
     * Find executions by user email
     */
    fun findByUserEmailOrderByCreatedAtDesc(
        userEmail: String,
        pageable: Pageable
    ): Page<PromptExecution>

    /**
     * Find executions within date range
     */
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<PromptExecution>

    /**
     * Get execution statistics for a prompt type
     */
    @Query(
        """
        SELECT 
            COUNT(e) as total,
            AVG(e.responseTimeMs) as avgResponseTime,
            AVG(e.totalTokens) as avgTokens,
            AVG(CASE WHEN e.userRating IS NOT NULL THEN e.userRating ELSE NULL END) as avgRating,
            SUM(CASE WHEN e.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount,
            SUM(CASE WHEN e.status = 'FAILURE' THEN 1 ELSE 0 END) as failureCount
        FROM PromptExecution e
        WHERE e.promptType = :promptType
        """
    )
    fun getStatisticsByPromptType(@Param("promptType") promptType: String): List<Any>

    /**
     * Get execution statistics for a specific version
     */
    @Query(
        """
        SELECT 
            COUNT(e) as total,
            AVG(e.responseTimeMs) as avgResponseTime,
            AVG(e.totalTokens) as avgTokens,
            AVG(CASE WHEN e.userRating IS NOT NULL THEN e.userRating ELSE NULL END) as avgRating,
            SUM(CASE WHEN e.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount
        FROM PromptExecution e
        WHERE e.promptType = :promptType AND e.promptVersion = :version
        """
    )
    fun getStatisticsByPromptTypeAndVersion(
        @Param("promptType") promptType: String,
        @Param("version") version: Int
    ): List<Any>

    /**
     * Count executions by prompt type
     */
    fun countByPromptType(promptType: String): Long

    /**
     * Count executions by prompt type and version
     */
    fun countByPromptTypeAndPromptVersion(promptType: String, promptVersion: Int): Long

    /**
     * Count executions by status
     */
    fun countByPromptTypeAndStatus(promptType: String, status: ExecutionStatus): Long

    /**
     * Get average rating for a prompt type
     */
    @Query(
        """
        SELECT AVG(e.userRating) 
        FROM PromptExecution e 
        WHERE e.promptType = :promptType AND e.userRating IS NOT NULL
        """
    )
    fun getAverageRating(@Param("promptType") promptType: String): Double?

    /**
     * Get average response time for a prompt type
     */
    @Query(
        """
        SELECT AVG(e.responseTimeMs) 
        FROM PromptExecution e 
        WHERE e.promptType = :promptType AND e.responseTimeMs IS NOT NULL
        """
    )
    fun getAverageResponseTime(@Param("promptType") promptType: String): Double?

    /**
     * Get recent executions with feedback
     */
    @Query(
        """
        SELECT e FROM PromptExecution e 
        WHERE e.promptType = :promptType 
        AND (e.userRating IS NOT NULL OR e.userFeedback IS NOT NULL)
        ORDER BY e.createdAt DESC
        """
    )
    fun getExecutionsWithFeedback(
        @Param("promptType") promptType: String,
        pageable: Pageable
    ): Page<PromptExecution>

    /**
     * Get daily execution counts
     */
    @Query(
        """
        SELECT DATE(e.createdAt) as date, COUNT(e) as count
        FROM PromptExecution e
        WHERE e.promptType = :promptType
        AND e.createdAt >= :startDate
        GROUP BY DATE(e.createdAt)
        ORDER BY DATE(e.createdAt) DESC
        """
    )
    fun getDailyExecutionCounts(
        @Param("promptType") promptType: String,
        @Param("startDate") startDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Get top rated executions
     */
    fun findTop10ByPromptTypeAndUserRatingIsNotNullOrderByUserRatingDescCreatedAtDesc(
        promptType: String
    ): List<PromptExecution>
}
