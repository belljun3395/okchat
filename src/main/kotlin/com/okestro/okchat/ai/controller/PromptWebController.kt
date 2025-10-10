package com.okestro.okchat.ai.controller

import com.okestro.okchat.ai.repository.PromptRepository
import com.okestro.okchat.ai.service.PromptService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

/**
 * Web UI controller for prompt management
 */
@Controller
@RequestMapping("/admin/prompts")
class PromptWebController(
    private val promptService: PromptService,
    private val promptRepository: PromptRepository
) {

    /**
     * Prompt management home page
     * Displays all prompt types with their latest versions
     */
    @GetMapping
    fun index(model: Model): String = runBlocking {
        log.info { "Loading prompt management page" }

        // Get all prompts and group by type
        val allPrompts = promptRepository.findAll()
        val promptsByType = allPrompts.filter { it.active }
            .groupBy { it.type }
            .mapValues { entry ->
                entry.value.maxByOrNull { it.version }
            }

        // Calculate statistics
        val totalTypes = promptsByType.size
        val totalVersions = allPrompts.size
        val activePrompts = allPrompts.count { it.active }

        model.addAttribute("promptsByType", promptsByType)
        model.addAttribute("totalTypes", totalTypes)
        model.addAttribute("totalVersions", totalVersions)
        model.addAttribute("activePrompts", activePrompts)

        return@runBlocking "prompts/index"
    }

    /**
     * Create new prompt page
     */
    @GetMapping("/create")
    fun create(model: Model): String {
        log.info { "Loading prompt creation page" }
        model.addAttribute("mode", "create")
        return "prompts/editor"
    }

    /**
     * Edit prompt page
     */
    @GetMapping("/edit/{type}")
    fun edit(@PathVariable type: String, model: Model): String = runBlocking {
        log.info { "Loading prompt edit page: $type" }

        val prompt = promptRepository.findLatestByTypeAndActive(type)
        if (prompt == null) {
            model.addAttribute("error", "Prompt type not found: $type")
            return@runBlocking "error"
        }

        model.addAttribute("mode", "edit")
        model.addAttribute("prompt", prompt)
        model.addAttribute("type", type)

        return@runBlocking "prompts/editor"
    }

    /**
     * View prompt history/versions
     */
    @GetMapping("/history/{type}")
    fun history(@PathVariable type: String, model: Model): String = runBlocking {
        log.info { "Loading prompt history: $type" }

        val versions = promptService.getAllVersions(type)
        if (versions.isEmpty()) {
            model.addAttribute("error", "No versions found for prompt type: $type")
            return@runBlocking "error"
        }

        model.addAttribute("type", type)
        model.addAttribute("versions", versions)
        model.addAttribute("totalVersions", versions.size)

        return@runBlocking "prompts/history"
    }

    /**
     * View prompt analytics and execution history
     */
    @GetMapping("/analytics/{type}")
    fun analytics(@PathVariable type: String, model: Model): String = runBlocking {
        log.info { "Loading prompt analytics: $type" }

        val prompt = promptRepository.findLatestByTypeAndActive(type)
        if (prompt == null) {
            model.addAttribute("error", "Prompt type not found: $type")
            return@runBlocking "error"
        }

        model.addAttribute("type", type)
        model.addAttribute("prompt", prompt)

        return@runBlocking "prompts/analytics"
    }
}
