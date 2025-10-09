package com.okestro.okchat.email.repository

import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingEmailReplyRepository : JpaRepository<PendingEmailReply, Long> {

    /**
     * Find all pending email replies by status (list)
     */
    fun findByStatusOrderByCreatedAtDesc(status: ReviewStatus): List<PendingEmailReply>

    /**
     * Find all pending email replies ordered by created date
     */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<PendingEmailReply>

    /**
     * Count pending emails by status
     */
    fun countByStatus(status: ReviewStatus): Long
}
