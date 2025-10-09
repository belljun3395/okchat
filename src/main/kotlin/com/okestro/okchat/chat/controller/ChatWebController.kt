package com.okestro.okchat.chat.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

/**
 * Web UI controller for chat interface
 */
@Controller
@RequestMapping("/chat")
class ChatWebController {

    /**
     * Chat interface page
     */
    @GetMapping
    fun index(model: Model): String {
        log.info { "Loading chat interface page" }
        return "chat/index"
    }
}
