package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipelineStep
import com.okestro.okchat.chat.pipeline.copy
import com.okestro.okchat.search.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Build context from search results
 * Organizes documents by relevance and formats for AI
 * OPTIMIZED: Focus on top results with clear hierarchy
 */
@Component
@Order(3) // After ReRankingStep (@Order(2))
class ContextBuildingStep(
    @Value("\${confluence.base-url}") private val confluenceBaseUrl: String
) : DocumentChatPipelineStep {

    companion object {
        private const val TOP_RESULTS_FOR_CONTEXT = 20 // Increased to capture more meeting records
        private const val HIGH_RELEVANCE_THRESHOLD = 0.7 // Cosine similarity range: 0-1, 0.7+ is good match
        private const val MEDIUM_RELEVANCE_THRESHOLD = 0.5 // 0.5+ is decent match
        private const val MAX_CONTENT_LENGTH = 1000 // Reduced to fit more documents in token limit
        private const val MAX_OTHER_RESULTS_PREVIEW = 10 // Increased to show more related documents
        private val DATE_PATTERN = Regex("""(\d{6})""")
    }
    override fun getStepName(): String = "Context Building"

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Building context from search results" }

        val searchResults = context.search?.results
        if (searchResults.isNullOrEmpty()) {
            log.warn { "[${getStepName()}] No search results available - returning empty context" }
            // Keep search context as-is (without contextText)
            return context
        }

        val topResults = searchResults.take(TOP_RESULTS_FOR_CONTEXT)
        log.info { "[${getStepName()}] Using top ${topResults.size} documents for context" }
        if (log.isDebugEnabled()) {
            logTopResults(topResults)
        }

        val userQuestion = context.input.message
        val validResults = topResults.filter { it.content.length > 100 }
        if (topResults.size > validResults.size) {
            val filtered = topResults.filter { it.content.length <= 100 }
            log.info { "[${getStepName()}] Content filtering: ${topResults.size} → ${validResults.size} results (filtered ${filtered.size} minimal docs)" }
            if (log.isDebugEnabled()) {
                filtered.take(5).forEach {
                    log.debug { "    - ${it.title} (content: ${it.content.length} chars)" }
                }
            }
        } else {
            log.debug { "[${getStepName()}] Content filtering: ${topResults.size} → ${validResults.size} results" }
        }

        val highRelevance = validResults.filter { it.score.value >= HIGH_RELEVANCE_THRESHOLD }
        val mediumRelevance = validResults.filter { it.score.value >= MEDIUM_RELEVANCE_THRESHOLD && it.score.value < HIGH_RELEVANCE_THRESHOLD }
        val otherResults = validResults.filter { it.score.value < MEDIUM_RELEVANCE_THRESHOLD }

        log.info { "[${getStepName()}] Relevance distribution: High=${highRelevance.size}, Medium=${mediumRelevance.size}, Other=${otherResults.size}" }

        val contextText = buildString {
            appendHeader(userQuestion, validResults.size, highRelevance.size)
            appendHighRelevanceDocuments(highRelevance)
            appendMediumRelevanceDocuments(mediumRelevance)
            appendOtherResults(otherResults)
        }

        log.info { "[${getStepName()}] Built context: ${contextText.length} chars" }
        log.debug { "[${getStepName()}] Context detail:\n" + contextText }

        return context.copy(
            search = context.search.copy(contextText = contextText)
        )
    }

    private fun StringBuilder.appendHeader(question: String, totalCount: Int, highCount: Int) {
        append("=== Search Results Analysis ===\n")
        append("Question: $question\n")
        append("Total $totalCount documents found")
        if (highCount > 0) {
            append(" (High relevance: $highCount)")
        }
        append("\n\n")
    }

    private fun StringBuilder.appendHighRelevanceDocuments(documents: List<SearchResult>) {
        if (documents.isEmpty()) return

        append("========================================\n")
        append("High Relevance Documents (${documents.size})\n")
        append("========================================\n")
        append("IMPORTANT: These documents are most relevant to the question.\n")
        append("IMPORTANT: **Analyze all ${documents.size} documents below to provide your answer!**\n\n")

        documents.forEachIndexed { index, result ->
            appendDocumentInfo(index + 1, result, detailed = true)
        }
        append("\n")
    }

    private fun StringBuilder.appendMediumRelevanceDocuments(documents: List<SearchResult>) {
        if (documents.isEmpty()) return

        append("Medium Relevance Documents (${documents.size}):\n\n")
        documents.forEachIndexed { index, result ->
            appendDocumentInfo(index + 1, result, detailed = true)
        }
        append("\n")
    }

    private fun StringBuilder.appendOtherResults(documents: List<SearchResult>) {
        if (documents.isEmpty()) return

        append("Other Related Documents (${documents.size}):\n")
        documents.take(MAX_OTHER_RESULTS_PREVIEW).forEach { result ->
            append("- ${result.title} (score: ${"%.2f".format(result.score.value)})\n")
        }
        if (documents.size > MAX_OTHER_RESULTS_PREVIEW) {
            append("... and ${documents.size - MAX_OTHER_RESULTS_PREVIEW} more\n")
        }
        append("\n")
    }

    private fun StringBuilder.appendDocumentInfo(index: Int, result: SearchResult, detailed: Boolean) {
        val isPdfAttachment = result.type == "confluence-pdf-attachment"
        // For PDF attachments, use pageId (parent page). For regular pages, use id.
        val linkPageId = if (isPdfAttachment && result.pageId.isNotBlank()) {
            result.pageId
        } else {
            result.id
        }
        val pageUrl = buildConfluencePageUrl(result.spaceKey, linkPageId)

        append("\n")
        append("===================================\n")
        if (isPdfAttachment) {
            append("Document $index: ${result.title} 📄 [PDF]\n")
        } else {
            append("Document $index: ${result.title}\n")
        }
        append("===================================\n")
        if (isPdfAttachment) {
            append("   Type: PDF Attachment\n")
        }
        append("   Link: $pageUrl\n")
        append("   Path: ${result.path}\n")
        append("   Relevance: ${"%.2f".format(result.score.value)}")

        extractAndAppendDate(result.title)
        append("\n")

        if (result.keywords.isNotBlank()) {
            append("   Keywords: ${result.keywords}\n")
        }

        if (detailed) {
            appendContent(result.content)
        }

        append("\n")
    }

    private fun StringBuilder.extractAndAppendDate(title: String) {
        val dateMatch = DATE_PATTERN.find(title) ?: return

        val dateStr = dateMatch.value
        val year = "20${dateStr.substring(0, 2)}"
        val month = dateStr.substring(2, 4)
        val day = dateStr.substring(4, 6)
        append(" | Date: $year-$month-$day")
    }

    private fun StringBuilder.appendContent(content: String) {
        append("   Content:\n")
        append("   ${content.replace("\n", "\n   ")}\n")
    }

    private fun buildConfluencePageUrl(spaceKey: String, pageId: String): String {
        val baseDomain = confluenceBaseUrl.substringBefore("/api/v2")
        val actualPageId = if (pageId.contains("_chunk_")) {
            pageId.substringBefore("_chunk_")
        } else {
            pageId
        }
        return "$baseDomain/spaces/$spaceKey/pages/$actualPageId"
    }

    private fun logTopResults(topResults: List<SearchResult>) {
        log.debug { "[${getStepName()}] ━━━ All ${topResults.size} documents selected for context ━━━" }
        topResults.forEachIndexed { index, result ->
            log.debug { "[${index + 1}/${topResults.size}] ${result.title}" }
            log.debug { "Score: ${"%.4f".format(result.score.value)}, ID: ${result.id}" }
            log.debug { "Content: ${result.content.length} chars" }
            log.debug { "Preview: ${result.content.take(150).replace("\n", " ")}..." }
        }
        log.debug { "[${getStepName()}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
    }
}
