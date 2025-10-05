package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Build dynamic system prompts based on query type
 * Loads prompt templates from external files for easy customization
 */
@Component
class DynamicPromptBuilder {

    private val basePrompt: String by lazy { loadPromptTemplate("base.txt") }
    private val commonGuidelines: String by lazy { loadPromptTemplate("common-guidelines.txt") }

    private val promptCache = mutableMapOf<QueryClassifier.QueryType, String>()

    init {
        log.info { "DynamicPromptBuilder initialized with externalized prompt templates" }
    }

    fun buildPrompt(queryType: QueryClassifier.QueryType): String {
        // Cache prompts for performance
        return promptCache.getOrPut(queryType) {
            val specificGuidance = loadSpecificPrompt(queryType)
            "$basePrompt\n\n$specificGuidance\n$commonGuidelines"
        }
    }

    private fun loadSpecificPrompt(queryType: QueryClassifier.QueryType): String {
        val filename = when (queryType) {
            QueryClassifier.QueryType.MEETING_RECORDS -> "meeting-records.txt"
            QueryClassifier.QueryType.PROJECT_STATUS -> "project-status.txt"
            QueryClassifier.QueryType.HOW_TO -> "how-to.txt"
            QueryClassifier.QueryType.INFORMATION -> "information.txt"
            QueryClassifier.QueryType.DOCUMENT_SEARCH -> "document-search.txt"
            QueryClassifier.QueryType.GENERAL -> "general.txt"
        }
        return loadPromptTemplate(filename)
    }

    private fun loadPromptTemplate(filename: String): String {
        return try {
            val resource = ClassPathResource("prompts/$filename")
            resource.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            log.error(e) { "Failed to load prompt template: prompts/$filename" }
            throw IllegalStateException("Could not load prompt template: prompts/$filename", e)
        }
    }
}
