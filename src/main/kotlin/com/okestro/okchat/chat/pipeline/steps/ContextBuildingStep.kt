package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.OptionalChatPipelineStep
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
) : OptionalChatPipelineStep {

    companion object {
        private const val TOP_RESULTS_FOR_CONTEXT = 20 // Increased to capture more meeting records
        private const val HIGH_RELEVANCE_THRESHOLD = 0.7 // Cosine similarity range: 0-1, 0.7+ is good match
        private const val MEDIUM_RELEVANCE_THRESHOLD = 0.5 // 0.5+ is decent match
        private const val MAX_CONTENT_LENGTH = 1000 // Reduced to fit more documents in token limit
        private const val MAX_OTHER_RESULTS_PREVIEW = 10 // Increased to show more related documents
        private val DATE_PATTERN = Regex("""(\d{6})""")
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Building context from search results" }

        val searchResults = context.searchResults
        if (searchResults.isNullOrEmpty()) {
            log.warn { "[${getStepName()}] No search results available - returning empty context" }
            // Return null contextText so PromptGenerationStep knows to use fallback
            return context.copy(contextText = null)
        }

        val topResults = searchResults.take(TOP_RESULTS_FOR_CONTEXT)
        log.info { "[${getStepName()}] Using top ${topResults.size} documents for context" }

        // Log ALL top documents in detail
        log.info { "[${getStepName()}] ━━━ All ${topResults.size} documents selected for context ━━━" }
        topResults.forEachIndexed { index, result ->
            log.info { "  [${index + 1}/${topResults.size}] ${result.title}" }
            log.info { "       Score: ${"%.4f".format(result.score.value)}, ID: ${result.id}" }
            log.info { "       Content: ${result.content.length} chars" }
            log.info { "       Preview: ${result.content.take(150).replace("\n", " ")}..." }
        }
        log.info { "[${getStepName()}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }

        val contextText = buildContextText(topResults, context.userMessage)
        log.info { "[${getStepName()}] Built context: ${contextText.length} chars" }
        log.info {
            "[${getStepName()}] Context :\n" + contextText
        }

        return context.copy(contextText = contextText)
    }

    private fun buildContextText(results: List<SearchResult>, userQuestion: String): String {
        // Filter out documents with minimal content (metadata-only chunks)
        val validResults = results.filter { it.content.length > 100 }

        log.info { "[${getStepName()}] Content filtering: ${results.size} → ${validResults.size} results" }
        if (results.size > validResults.size) {
            val filtered = results.filter { it.content.length <= 100 }
            log.info { "[${getStepName()}] Filtered out ${filtered.size} minimal content documents:" }
            filtered.take(5).forEach {
                log.info { "    - ${it.title} (content: ${it.content.length} chars)" }
            }
        }

        val highRelevance = validResults.filter { it.score.value >= HIGH_RELEVANCE_THRESHOLD }
        val mediumRelevance = validResults.filter { it.score.value >= MEDIUM_RELEVANCE_THRESHOLD && it.score.value < HIGH_RELEVANCE_THRESHOLD }
        val otherResults = validResults.filter { it.score.value < MEDIUM_RELEVANCE_THRESHOLD }

        log.info { "[${getStepName()}] Relevance distribution: High=${highRelevance.size}, Medium=${mediumRelevance.size}, Other=${otherResults.size}" }

        return buildString {
            appendHeader(userQuestion, validResults.size, highRelevance.size)
            appendHighRelevanceDocuments(highRelevance)
            appendMediumRelevanceDocuments(mediumRelevance)
            appendOtherResults(otherResults)
            appendImportantInstruction()
        }
    }

    private fun StringBuilder.appendHeader(question: String, totalCount: Int, highCount: Int) {
        append("=== 🎯 검색 결과 분석 ===\n")
        append("질문: $question\n")
        append("총 ${totalCount}개 문서 발견")
        if (highCount > 0) {
            append(" (고관련성: ${highCount}개)")
        }
        append("\n\n")
    }

    private fun StringBuilder.appendHighRelevanceDocuments(documents: List<SearchResult>) {
        if (documents.isEmpty()) return

        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("🎯 고관련성 문서 (${documents.size}개)\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("⚠️ 다음 문서들이 질문과 가장 관련이 높습니다.\n")
        append("⚠️ **중요: 아래 ${documents.size}개 문서를 모두 분석하여 답변하세요!**\n\n")

        documents.forEachIndexed { index, result ->
            appendDocumentInfo(index + 1, result, detailed = true)
        }
        append("\n")
    }

    private fun StringBuilder.appendMediumRelevanceDocuments(documents: List<SearchResult>) {
        if (documents.isEmpty()) return

        append("📌 중관련성 문서 (${documents.size}개):\n\n")
        documents.forEachIndexed { index, result ->
            appendDocumentInfo(index + 1, result, detailed = true)
        }
        append("\n")
    }

    private fun StringBuilder.appendOtherResults(documents: List<SearchResult>) {
        if (documents.isEmpty()) return

        append("📄 기타 관련 문서 (${documents.size}개):\n")
        documents.take(MAX_OTHER_RESULTS_PREVIEW).forEach { result ->
            append("- ${result.title} (점수: ${"%.2f".format(result.score.value)})\n")
        }
        if (documents.size > MAX_OTHER_RESULTS_PREVIEW) {
            append("... 외 ${documents.size - MAX_OTHER_RESULTS_PREVIEW}개\n")
        }
        append("\n")
    }

    private fun StringBuilder.appendImportantInstruction() {
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("⚠️ 중요 지침:\n")
        append("1. 위의 **모든** 검색 결과를 확인하세요 (하나만 보지 마세요!)\n")
        append("2. 회의록 요약 시 검색된 모든 회의를 포함하세요\n")
        append("3. 각 문서의 내용을 빠짐없이 분석하세요\n")
        append("4. 정보가 충분한 경우 도구(tool)를 사용하지 마세요\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
    }

    private fun StringBuilder.appendDocumentInfo(index: Int, result: SearchResult, detailed: Boolean) {
        val pageUrl = buildConfluencePageUrl(result.spaceKey, result.id)

        append("\n")
        append("═══════════════════════════════════\n")
        append("📄 문서 $index: ${result.title}\n")
        append("═══════════════════════════════════\n")
        append("   링크: $pageUrl\n")
        append("   경로: ${result.path}\n")
        append("   관련도: ${"%.2f".format(result.score.value)}")

        extractAndAppendDate(result.title)
        append("\n")

        if (result.keywords.isNotBlank()) {
            append("   키워드: ${result.keywords}\n")
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
        append(" | 날짜: $year-$month-$day")
    }

    private fun StringBuilder.appendContent(content: String) {
        val contentPreview = if (content.length > MAX_CONTENT_LENGTH) {
            content.take(MAX_CONTENT_LENGTH) + "\n   [... 내용 생략 ...]"
        } else {
            content
        }
        append("   내용:\n")
        append("   ${contentPreview.replace("\n", "\n   ")}\n")
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

    override fun getStepName(): String = "Context Building"
}
