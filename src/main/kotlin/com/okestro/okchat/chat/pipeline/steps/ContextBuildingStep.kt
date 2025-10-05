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
@Order(2)
class ContextBuildingStep(
    @Value("\${confluence.base-url}") private val confluenceBaseUrl: String
) : OptionalChatPipelineStep {

    companion object {
        private const val TOP_RESULTS_FOR_CONTEXT = 10 // Optimized: reduced from 30 to fit token limits
        private const val HIGH_RELEVANCE_THRESHOLD = 1.2 // similarity (0~1) + boost (0.2~2.0) = 0.2~3.0
        private const val MEDIUM_RELEVANCE_THRESHOLD = 0.8 // 0.8 이상이면 괜찮은 매칭
        private const val MAX_CONTENT_LENGTH = 1500 // Optimized: reduced from 3000 to save tokens
        private const val MAX_OTHER_RESULTS_PREVIEW = 5
        private val DATE_PATTERN = Regex("""(\d{6})""")
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Building context from search results" }

        val searchResults = context.searchResults
        if (searchResults.isNullOrEmpty()) {
            log.warn { "[${getStepName()}] No search results available" }
            return context.copy(contextText = "관련 Confluence 페이지를 찾을 수 없습니다.")
        }

        val topResults = searchResults.take(TOP_RESULTS_FOR_CONTEXT)
        log.info { "[${getStepName()}] Using top ${topResults.size} documents" }

        val contextText = buildContextText(topResults, context.userMessage)

        return context.copy(contextText = contextText)
    }

    private fun buildContextText(results: List<SearchResult>, userQuestion: String): String {
        val highRelevance = results.filter { it.score.value >= HIGH_RELEVANCE_THRESHOLD }
        val mediumRelevance = results.filter { it.score.value >= MEDIUM_RELEVANCE_THRESHOLD && it.score.value < HIGH_RELEVANCE_THRESHOLD }
        val otherResults = results.filter { it.score.value < MEDIUM_RELEVANCE_THRESHOLD }

        return buildString {
            appendHeader(userQuestion, results.size, highRelevance.size)
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
        append("⚠️ 다음 문서들이 질문과 가장 관련이 높습니다. 우선적으로 참고하세요!\n\n")

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
        append("1. 위의 검색 결과를 **반드시 먼저** 확인하세요\n")
        append("2. 고관련성 문서에 답이 있으면 그것을 기반으로 답변하세요\n")
        append("3. 정보가 부족한 경우에만 도구(tool)를 사용하세요\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
    }

    private fun StringBuilder.appendDocumentInfo(index: Int, result: SearchResult, detailed: Boolean) {
        val pageUrl = buildConfluencePageUrl(result.spaceKey, result.id)

        append("$index. ${result.title}\n")
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
