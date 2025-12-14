package com.okestro.okchat.email.application.dto

import com.okestro.okchat.email.model.entity.PendingEmailReply
import org.springframework.data.domain.Page

data class GetPendingRepliesUseCaseIn(
    val page: Int = 0,
    val size: Int = 20
)

data class GetPendingRepliesUseCaseOut(
    val replies: Page<PendingEmailReply>
)
