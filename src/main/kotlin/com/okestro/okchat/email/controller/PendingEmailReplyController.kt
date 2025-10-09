package com.okestro.okchat.email.controller

import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.service.PendingEmailReplyService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * REST API controller for managing pending email replies
 */
@RestController
@RequestMapping("/api/email/pending")
class PendingEmailReplyController(
    private val pendingEmailReplyService: PendingEmailReplyService
) {

    /**
     * Get all pending email replies with pagination
     */
    @GetMapping
    fun getPendingReplies(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PendingEmailReply>> {
        logger.info { "Getting pending email replies: page=$page, size=$size" }
        val replies = pendingEmailReplyService.getPendingReplies(page, size)
        return ResponseEntity.ok(replies)
    }

    /**
     * Get pending email replies by status
     */
    @GetMapping("/status/{status}")
    fun getPendingRepliesByStatus(
        @PathVariable status: ReviewStatus
    ): ResponseEntity<List<PendingEmailReply>> {
        logger.info { "Getting pending email replies by status: $status" }
        val replies = pendingEmailReplyService.getPendingRepliesByStatus(status)
        return ResponseEntity.ok(replies)
    }

    /**
     * Get a specific pending email reply by ID
     */
    @GetMapping("/{id}")
    fun getPendingReplyById(
        @PathVariable id: Long
    ): ResponseEntity<PendingEmailReply> {
        logger.info { "Getting pending email reply: id=$id" }
        val reply = pendingEmailReplyService.getPendingReplyById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(reply)
    }

    /**
     * Get counts by status
     */
    @GetMapping("/count")
    fun getCounts(): ResponseEntity<Map<String, Long>> {
        logger.info { "Getting counts by status" }
        val counts = mapOf(
            "pending" to pendingEmailReplyService.countByStatus(ReviewStatus.PENDING),
            "approved" to pendingEmailReplyService.countByStatus(ReviewStatus.APPROVED),
            "rejected" to pendingEmailReplyService.countByStatus(ReviewStatus.REJECTED),
            "sent" to pendingEmailReplyService.countByStatus(ReviewStatus.SENT),
            "failed" to pendingEmailReplyService.countByStatus(ReviewStatus.FAILED)
        )
        return ResponseEntity.ok(counts)
    }

    /**
     * Approve and send an email reply
     */
    @PostMapping("/{id}/approve")
    fun approveAndSend(
        @PathVariable id: Long,
        @RequestBody request: ReviewRequest
    ): ResponseEntity<ApiResponse> = runBlocking {
        logger.info { "Approving email reply: id=$id, reviewedBy=${request.reviewedBy}" }
        val result = pendingEmailReplyService.approveAndSend(id, request.reviewedBy)

        result.fold(
            onSuccess = { reply ->
                ResponseEntity.ok(ApiResponse(
                    success = true,
                    message = "Email approved and sent successfully",
                    data = reply
                ))
            },
            onFailure = { error ->
                logger.error(error) { "Failed to approve and send email: id=$id" }
                ResponseEntity.badRequest().body(ApiResponse(
                    success = false,
                    message = error.message ?: "Failed to approve and send email",
                    data = null
                ))
            }
        )
    }

    /**
     * Reject an email reply
     */
    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: Long,
        @RequestBody request: ReviewRequest
    ): ResponseEntity<ApiResponse> = runBlocking {
        logger.info { "Rejecting email reply: id=$id, reviewedBy=${request.reviewedBy}" }
        val result = pendingEmailReplyService.reject(
            id = id,
            reviewedBy = request.reviewedBy,
            rejectionReason = request.rejectionReason
        )

        result.fold(
            onSuccess = { reply ->
                ResponseEntity.ok(ApiResponse(
                    success = true,
                    message = "Email rejected successfully",
                    data = reply
                ))
            },
            onFailure = { error ->
                logger.error(error) { "Failed to reject email: id=$id" }
                ResponseEntity.badRequest().body(ApiResponse(
                    success = false,
                    message = error.message ?: "Failed to reject email",
                    data = null
                ))
            }
        )
    }

    /**
     * Delete a pending email reply
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse> {
        logger.info { "Deleting pending email reply: id=$id" }
        try {
            pendingEmailReplyService.delete(id)
            return ResponseEntity.ok(ApiResponse(
                success = true,
                message = "Email reply deleted successfully",
                data = null
            ))
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete email reply: id=$id" }
            return ResponseEntity.badRequest().body(ApiResponse(
                success = false,
                message = e.message ?: "Failed to delete email reply",
                data = null
            ))
        }
    }
}

/**
 * Request body for reviewing emails
 */
data class ReviewRequest(
    val reviewedBy: String,
    val rejectionReason: String? = null
)

/**
 * Generic API response
 */
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any?
)
