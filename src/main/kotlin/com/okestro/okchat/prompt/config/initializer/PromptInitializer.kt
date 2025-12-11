package com.okestro.okchat.prompt.config.initializer

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import com.okestro.okchat.prompt.application.CheckPromptExistsUseCase
import com.okestro.okchat.prompt.application.CreatePromptUseCase
import com.okestro.okchat.prompt.application.dto.CheckPromptExistsUseCaseIn
import com.okestro.okchat.prompt.application.dto.CreatePromptUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Initialize prompts from text files to database on application startup
 * Only runs if prompts don't exist in the database
 */
@Component
class PromptInitializer(
    private val checkPromptExistsUseCase: CheckPromptExistsUseCase,
    private val createPromptUseCase: CreatePromptUseCase
) : ApplicationRunner {

    companion object {
        private const val PROMPT_BASE_PATH = "prompts"

        private val PROMPT_TYPE_MAPPING = mapOf(
            QueryClassifier.QueryType.BASE.name to "base",
            QueryClassifier.QueryType.COMMON_GUIDELINES.name to "common-guidelines",
            QueryClassifier.QueryType.MEETING_RECORDS.name to "meeting-records",
            QueryClassifier.QueryType.PROJECT_STATUS.name to "project-status",
            QueryClassifier.QueryType.HOW_TO.name to "how-to",
            QueryClassifier.QueryType.INFORMATION.name to "information",
            QueryClassifier.QueryType.DOCUMENT_SEARCH.name to "document-search",
            QueryClassifier.QueryType.GENERAL.name to "general"
        )
    }

    override fun run(args: ApplicationArguments?) = runBlocking {
        log.info { "Starting prompt initialization from text files..." }

        var initialized = 0
        var skipped = 0

        PROMPT_TYPE_MAPPING.forEach { (type, filename) ->
            try {
                if (!checkPromptExistsUseCase.execute(CheckPromptExistsUseCaseIn(type)).exists) {
                    val content = loadPromptFromFile(filename)
                    createPromptUseCase.execute(CreatePromptUseCaseIn(type, content))
                    initialized++
                    log.info { "Initialized prompt: $type from $filename.txt" }
                } else {
                    skipped++
                    log.debug { "Skipped prompt initialization (already exists): $type" }
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to initialize prompt: $type from $filename.txt" }
            }
        }

        log.info { "Prompt initialization completed: $initialized initialized, $skipped skipped" }
    }

    private fun loadPromptFromFile(filename: String): String {
        return try {
            val resource = ClassPathResource("$PROMPT_BASE_PATH/$filename.txt")
            resource.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            log.error(e) { "Failed to load prompt file: $filename.txt" }
            throw IllegalStateException("Could not load prompt file: $filename.txt", e)
        }
    }
}
