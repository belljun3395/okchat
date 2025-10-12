package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.PendingEmailReply
import com.okestro.okchat.email.model.ReviewStatus

data class GetPendingRepliesByStatusUseCaseIn(
    val status: ReviewStatus
)

data class GetPendingRepliesByStatusUseCaseOut(
    val replies: List<PendingEmailReply>
)
