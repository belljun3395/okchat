package com.okestro.okchat.chat.repository

import com.okestro.okchat.chat.model.entity.ChatInteraction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChatInteractionRepository : JpaRepository<ChatInteraction, Long> {

    /**
     * Find interaction by request ID
     */
    fun findByRequestId(requestId: String): ChatInteraction?

    /**
     * Count interactions by date range
     */
    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    /**
     * Get average rating
     */
    @Query("SELECT AVG(c.userRating) FROM ChatInteraction c WHERE c.userRating IS NOT NULL AND c.createdAt BETWEEN :startDate AND :endDate")
    fun getAverageRating(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Double?

    /**
     * Count helpful interactions (wasHelpful = true)
     */
    @Query("SELECT COUNT(c) FROM ChatInteraction c WHERE c.wasHelpful = true AND c.createdAt BETWEEN :startDate AND :endDate")
    fun countHelpfulInteractions(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    /**
     * Count interactions with feedback (wasHelpful IS NOT NULL)
     */
    @Query("SELECT COUNT(c) FROM ChatInteraction c WHERE c.wasHelpful IS NOT NULL AND c.createdAt BETWEEN :startDate AND :endDate")
    fun countInteractionsWithFeedback(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    /**
     * Get average response time
     */
    @Query("SELECT AVG(c.responseTimeMs) FROM ChatInteraction c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    fun getAverageResponseTime(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Double?

    /**
     * Get query type statistics
     */
    @Query(
        """
        SELECT c.queryType as queryType, COUNT(c) as count, AVG(c.userRating) as avgRating, AVG(c.responseTimeMs) as avgResponseTime
        FROM ChatInteraction c 
        WHERE c.createdAt BETWEEN :startDate AND :endDate
        GROUP BY c.queryType
        ORDER BY count DESC
    """
    )
    fun getQueryTypeStats(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<QueryTypeStats>

    /**
     * Query type statistics interface
     */
    interface QueryTypeStats {
        val queryType: String
        val count: Long
        val avgRating: Double?
        val avgResponseTime: Double
    }
}
