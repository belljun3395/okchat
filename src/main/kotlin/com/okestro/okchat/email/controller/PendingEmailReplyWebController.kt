package com.okestro.okchat.email.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

private val logger = KotlinLogging.logger {}

/**
 * Web UI controller for pending email review interface
 */
@Controller
@RequestMapping("/email/review")
class PendingEmailReplyWebController {

    /**
     * Email review interface page
     */
    @GetMapping
    fun index(model: Model): String {
        logger.info { "Loading email review interface page" }
        return "email/review"
    }
}
