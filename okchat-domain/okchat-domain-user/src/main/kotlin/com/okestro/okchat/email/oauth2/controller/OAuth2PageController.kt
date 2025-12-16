package com.okestro.okchat.email.oauth2.controller

import com.okestro.okchat.email.oauth2.application.ExchangeOAuth2CodeUseCase
import com.okestro.okchat.email.oauth2.application.dto.ExchangeOAuth2CodeUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Controller
@RequestMapping("/oauth2")
class OAuth2PageController(
    private val exchangeOAuth2CodeUseCase: ExchangeOAuth2CodeUseCase
) {

    @GetMapping("/login")
    fun loginPage(): String {
        return "oauth2/login"
    }

    @GetMapping("/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) error_description: String?,
        model: Model
    ): Mono<String> {
        // Handle error from provider directly
        if (!error.isNullOrBlank()) {
            model.addAttribute("status", "error")
            model.addAttribute("message", "Provider Error: $error - ${error_description ?: "No details"}")
            return Mono.just("oauth2/result")
        }

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            model.addAttribute("status", "error")
            model.addAttribute("message", "Invalid request: Missing code or state parameter.")
            return Mono.just("oauth2/result")
        }

        return mono {
            try {
                exchangeOAuth2CodeUseCase.execute(
                    ExchangeOAuth2CodeUseCaseIn(state, code)
                )
                model.addAttribute("status", "success")
                model.addAttribute("message", "Successfully authenticated $state")
                model.addAttribute("email", state)
            } catch (e: Exception) {
                logger.error(e) { "Failed to exchange code for token" }
                model.addAttribute("status", "error")
                model.addAttribute("message", "Authentication failed: ${e.message}")
            }
            "oauth2/result"
        }
    }
}
