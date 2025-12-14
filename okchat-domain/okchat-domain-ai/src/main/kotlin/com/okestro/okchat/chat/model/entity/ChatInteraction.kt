package com.okestro.okchat.chat.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Chat interaction tracking entity
 * Tracks all chat interactions for quality analysis and improvement
 */
@Entity
@Table(
    name = "chat_interactions",
    indexes = [
        Index(name = "idx_chat_session_id", columnList = "session_id"),
        Index(name = "idx_chat_request_id", columnList = "request_id"),
        Index(name = "idx_chat_created_at", columnList = "created_at"),
        Index(name = "idx_chat_query_type", columnList = "query_type"),
        Index(name = "idx_chat_rating", columnList = "user_rating"),
        Index(name = "idx_chat_helpful", columnList = "was_helpful")
    ]
)
data class ChatInteraction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Request identification
    @Column(nullable = false, length = 100)
    val sessionId: String,

    @Column(nullable = false, unique = true, length = 100)
    val requestId: String,

    // Request information
    @Column(nullable = false, columnDefinition = "TEXT")
    val userMessage: String,

    @Column(nullable = false, length = 50)
    val queryType: String,

    @Column(columnDefinition = "TEXT")
    val extractedKeywords: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val aiResponse: String,

    @Column(nullable = false)
    val responseTimeMs: Long,

    @Column(nullable = false)
    val searchResultsCount: Int = 0,

    @Column(columnDefinition = "TEXT")
    val documentsUsed: String? = null,

    @Column(columnDefinition = "TEXT")
    val pipelineStepsExecuted: String? = null,

    @Column(length = 50)
    val llmModelUsed: String? = null,

    @Column
    val userRating: Int? = null,

    @Column(columnDefinition = "TEXT")
    val userFeedback: String? = null,

    @Column
    val wasHelpful: Boolean? = null,

    @Column
    val isDeepThink: Boolean = false,

    @Column(length = 100)
    val userEmail: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val feedbackAt: LocalDateTime? = null
)
