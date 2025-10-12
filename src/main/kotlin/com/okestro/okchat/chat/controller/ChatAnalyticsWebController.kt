package com.okestro.okchat.chat.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Web controller for Chat Analytics Dashboard UI
 */
@Controller
@RequestMapping("/admin/chat/analytics")
class ChatAnalyticsWebController {

    /**
     * Render analytics dashboard page
     * * @return Thymeleaf template name for analytics dashboard
     */
    @GetMapping
    fun analyticsPage(): String {
        return "analytics/dashboard"
    }
}
