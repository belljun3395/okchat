package com.okestro.okchat.ai.service

import com.okestro.okchat.ai.model.ExecutionStatus
import com.okestro.okchat.ai.model.PromptExecution
import com.okestro.okchat.ai.repository.PromptExecutionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PromptExecutionService(
    private val executionRepository: PromptExecutionRepository
) {
    private val log = KotlinLogging.logger {}

    /**
     * Record a prompt execution
     */
    @Transactional("transactionManager")
    suspend fun recordExecution(
        promptType: String,
        promptVersion: Int,
        promptContent: String,
        sessionId: String? = null,
        userEmail: String? = null,
        userInput: String? = null,
        inputVariables: String? = null,
        generatedOutput: String? = null,
        responseTimeMs: Long? = null,
        inputTokens: Int? = null,
        outputTokens: Int? = null,
        totalTokens: Int? = null,
        status: ExecutionStatus = ExecutionStatus.SUCCESS,
        errorMessage: String? = null,
        metadata: String? = null
    ): PromptExecution {
        val execution = PromptExecution(
            promptType = promptType,
            promptVersion = promptVersion,
            promptContent = promptContent,
            sessionId = sessionId,
            userEmail = userEmail,
            userInput = userInput,
            inputVariables = inputVariables,
            generatedOutput = generatedOutput,
            responseTimeMs = responseTimeMs,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = totalTokens,
            status = status,
            errorMessage = errorMessage,
            metadata = metadata,
            createdAt = LocalDateTime.now()
        )

        val saved = executionRepository.save(execution)
        log.info { "Recorded execution for prompt: $promptType v$promptVersion, status: $status" }
        return saved
    }

    /**
     * Add user feedback to an execution
     */
    @Transactional("transactionManager")
    suspend fun addFeedback(
        executionId: Long,
        rating: Int? = null,
        feedback: String? = null
    ): PromptExecution {
        val execution = executionRepository.findById(executionId)
            .orElseThrow { IllegalArgumentException("Execution not found: $executionId") }

        val updated = execution.copy(
            userRating = rating,
            userFeedback = feedback
        )

        val saved = executionRepository.save(updated)
        log.info { "Added feedback to execution $executionId: rating=$rating" }
        return saved
    }

    /**
     * Get execution history for a prompt type
     */
    suspend fun getExecutionHistory(
        promptType: String,
        page: Int = 0,
        size: Int = 50
    ): Page<PromptExecution> {
        return executionRepository.findByPromptTypeOrderByCreatedAtDesc(
            promptType,
            PageRequest.of(page, size)
        )
    }

    /**
     * Get execution history for a specific version
     */
    suspend fun getExecutionHistoryByVersion(
        promptType: String,
        version: Int,
        page: Int = 0,
        size: Int = 50
    ): Page<PromptExecution> {
        return executionRepository.findByPromptTypeAndPromptVersionOrderByCreatedAtDesc(
            promptType,
            version,
            PageRequest.of(page, size)
        )
    }

    /**
     * Get execution statistics for a prompt type
     */
    suspend fun getStatistics(promptType: String): PromptStatistics {
        val total = executionRepository.countByPromptType(promptType)
        val successCount = executionRepository.countByPromptTypeAndStatus(promptType, ExecutionStatus.SUCCESS)
        val failureCount = executionRepository.countByPromptTypeAndStatus(promptType, ExecutionStatus.FAILURE)
        val avgRating = executionRepository.getAverageRating(promptType) ?: 0.0
        val avgResponseTime = executionRepository.getAverageResponseTime(promptType) ?: 0.0

        val successRate = if (total > 0) (successCount.toDouble() / total * 100) else 0.0

        return PromptStatistics(
            totalExecutions = total,
            successCount = successCount,
            failureCount = failureCount,
            successRate = successRate,
            averageRating = avgRating,
            averageResponseTimeMs = avgResponseTime
        )
    }

    /**
     * Get version comparison statistics
     */
    suspend fun compareVersions(promptType: String): List<VersionStatistics> {
        val executions = executionRepository.findByPromptTypeOrderByCreatedAtDesc(
            promptType,
            PageRequest.of(0, 10000)
        )

        val versionGroups = executions.content.groupBy { it.promptVersion }

        return versionGroups.map { (version, execs) ->
            val total = execs.size.toLong()
            val successCount = execs.count { it.status == ExecutionStatus.SUCCESS }.toLong()
            val avgRating = execs.mapNotNull { it.userRating }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgResponseTime = execs.mapNotNull { it.responseTimeMs }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgTokens = execs.mapNotNull { it.totalTokens }.average().takeIf { !it.isNaN() } ?: 0.0

            VersionStatistics(
                version = version,
                totalExecutions = total,
                successCount = successCount,
                successRate = if (total > 0) (successCount.toDouble() / total * 100) else 0.0,
                averageRating = avgRating,
                averageResponseTimeMs = avgResponseTime,
                averageTokens = avgTokens
            )
        }.sortedByDescending { it.version }
    }

    /**
     * Get executions with user feedback
     */
    suspend fun getExecutionsWithFeedback(
        promptType: String,
        page: Int = 0,
        size: Int = 20
    ): Page<PromptExecution> {
        return executionRepository.getExecutionsWithFeedback(
            promptType,
            PageRequest.of(page, size)
        )
    }

    /**
     * Get daily execution trends
     */
    suspend fun getDailyTrends(
        promptType: String,
        days: Int = 30
    ): List<DailyTrend> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        val data = executionRepository.getDailyExecutionCounts(promptType, startDate)

        return data.map { row ->
            DailyTrend(
                date = row[0].toString(),
                count = (row[1] as Number).toLong()
            )
        }
    }

    /**
     * Get top rated executions
     */
    suspend fun getTopRatedExecutions(promptType: String): List<PromptExecution> {
        return executionRepository.findTop10ByPromptTypeAndUserRatingIsNotNullOrderByUserRatingDescCreatedAtDesc(
            promptType
        )
    }

    /**
     * Get recent executions by session
     */
    suspend fun getExecutionsBySession(
        sessionId: String,
        page: Int = 0,
        size: Int = 50
    ): Page<PromptExecution> {
        return executionRepository.findBySessionIdOrderByCreatedAtDesc(
            sessionId,
            PageRequest.of(page, size)
        )
    }
}

data class PromptStatistics(
    val totalExecutions: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double,
    val averageRating: Double,
    val averageResponseTimeMs: Double
)

data class VersionStatistics(
    val version: Int,
    val totalExecutions: Long,
    val successCount: Long,
    val successRate: Double,
    val averageRating: Double,
    val averageResponseTimeMs: Double,
    val averageTokens: Double
)

data class DailyTrend(
    val date: String,
    val count: Long
)
