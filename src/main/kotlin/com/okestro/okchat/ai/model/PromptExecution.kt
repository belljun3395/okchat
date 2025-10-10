package com.okestro.okchat.ai.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "prompt_executions",
    indexes = [
        Index(name = "idx_execution_prompt", columnList = "prompt_type,prompt_version"),
        Index(name = "idx_execution_session", columnList = "session_id"),
        Index(name = "idx_execution_created", columnList = "created_at"),
        Index(name = "idx_execution_status", columnList = "status")
    ]
)
data class PromptExecution(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "prompt_type", nullable = false, length = 100)
    val promptType: String,

    @Column(name = "prompt_version", nullable = false)
    val promptVersion: Int,

    @Column(name = "prompt_content", nullable = false, columnDefinition = "TEXT")
    val promptContent: String,

    @Column(name = "session_id", length = 100)
    val sessionId: String? = null,

    @Column(name = "user_email", length = 255)
    val userEmail: String? = null,

    @Column(name = "input_variables", columnDefinition = "TEXT")
    val inputVariables: String? = null,

    @Column(name = "user_input", columnDefinition = "TEXT")
    val userInput: String? = null,

    @Column(name = "generated_output", columnDefinition = "TEXT")
    val generatedOutput: String? = null,

    @Column(name = "response_time_ms")
    val responseTimeMs: Long? = null,

    @Column(name = "input_tokens")
    val inputTokens: Int? = null,

    @Column(name = "output_tokens")
    val outputTokens: Int? = null,

    @Column(name = "total_tokens")
    val totalTokens: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: ExecutionStatus = ExecutionStatus.SUCCESS,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "user_rating")
    val userRating: Int? = null, // 1-5 stars

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    val userFeedback: String? = null,

    @Column(name = "metadata", columnDefinition = "TEXT")
    val metadata: String? = null, // JSON for additional info

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class ExecutionStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
    TIMEOUT,
    CANCELLED
}
