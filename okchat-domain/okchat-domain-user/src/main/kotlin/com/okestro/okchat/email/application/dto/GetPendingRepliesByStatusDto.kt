package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus

data class GetPendingRepliesByStatusUseCaseIn(
    val status: ReviewStatus
)

data class GetPendingRepliesByStatusUseCaseOut(
    val replies: List<PendingEmailReply>
)
