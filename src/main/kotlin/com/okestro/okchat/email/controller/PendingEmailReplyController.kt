package com.okestro.okchat.email.controller

import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus
import com.okestro.okchat.email.service.PendingEmailReplyService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

/**
 * REST API controller for managing pending email replies
 */
@RestController
@RequestMapping("/api/email/pending")
@Tag(
    name = "Email API",
    description = "이메일 자동 응답 관리 API. AI가 생성한 이메일 답변을 검토하고 승인/거부할 수 있습니다."
)
class PendingEmailReplyController(
    private val pendingEmailReplyService: PendingEmailReplyService
) {

    /**
     * Get all pending email replies with pagination
     */
    @GetMapping
    @Operation(
        summary = "대기 중인 이메일 답변 목록 조회",
        description = "페이징 처리된 대기 중인 이메일 답변 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getPendingReplies(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int
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
    @Operation(
        summary = "이메일 답변 승인 및 발송",
        description = "대기 중인 이메일 답변을 승인하고 실제로 발송합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "승인 및 발송 성공"),
            ApiResponse(responseCode = "400", description = "발송 실패")
        ]
    )
    fun approveAndSend(
        @Parameter(description = "이메일 답변 ID", example = "1", required = true)
        @PathVariable
        id: Long,
        @Parameter(description = "검토 정보", required = true)
        @RequestBody
        request: ReviewRequest
    ): ResponseEntity<EmailApiResponse> = runBlocking {
        logger.info { "Approving email reply: id=$id, reviewedBy=${request.reviewedBy}" }
        val result = pendingEmailReplyService.approveAndSend(id, request.reviewedBy)

        result.fold(
            onSuccess = { reply ->
                ResponseEntity.ok(
                    EmailApiResponse(
                        success = true,
                        message = "Email approved and sent successfully",
                        data = reply
                    )
                )
            },
            onFailure = { error ->
                logger.error(error) { "Failed to approve and send email: id=$id" }
                ResponseEntity.badRequest().body(
                    EmailApiResponse(
                        success = false,
                        message = error.message ?: "Failed to approve and send email",
                        data = null
                    )
                )
            }
        )
    }

    /**
     * Reject an email reply
     */
    @PostMapping("/{id}/reject")
    @Operation(
        summary = "이메일 답변 거부",
        description = "대기 중인 이메일 답변을 거부합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "거부 성공"),
            ApiResponse(responseCode = "400", description = "거부 실패")
        ]
    )
    fun reject(
        @Parameter(description = "이메일 답변 ID", example = "1", required = true)
        @PathVariable
        id: Long,
        @Parameter(description = "검토 정보 (거부 사유 포함)", required = true)
        @RequestBody
        request: ReviewRequest
    ): ResponseEntity<EmailApiResponse> = runBlocking {
        logger.info { "Rejecting email reply: id=$id, reviewedBy=${request.reviewedBy}" }
        val result = pendingEmailReplyService.reject(
            id = id,
            reviewedBy = request.reviewedBy,
            rejectionReason = request.rejectionReason
        )

        result.fold(
            onSuccess = { reply ->
                ResponseEntity.ok(
                    EmailApiResponse(
                        success = true,
                        message = "Email rejected successfully",
                        data = reply
                    )
                )
            },
            onFailure = { error ->
                logger.error(error) { "Failed to reject email: id=$id" }
                ResponseEntity.badRequest().body(
                    EmailApiResponse(
                        success = false,
                        message = error.message ?: "Failed to reject email",
                        data = null
                    )
                )
            }
        )
    }

    /**
     * Delete a pending email reply
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<EmailApiResponse> {
        logger.info { "Deleting pending email reply: id=$id" }
        try {
            pendingEmailReplyService.delete(id)
            return ResponseEntity.ok(
                EmailApiResponse(
                    success = true,
                    message = "Email reply deleted successfully",
                    data = null
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete email reply: id=$id" }
            return ResponseEntity.badRequest().body(
                EmailApiResponse(
                    success = false,
                    message = e.message ?: "Failed to delete email reply",
                    data = null
                )
            )
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
data class EmailApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any?
)
