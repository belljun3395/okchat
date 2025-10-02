package com.okestro.okchat.chat.service

import com.okestro.okchat.ai.support.KeywordExtractionService
import com.okestro.okchat.search.service.DocumentSearchService
import com.okestro.okchat.search.service.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

private val log = KotlinLogging.logger {}

@Service
class ChatService(
    private val chatClient: ChatClient,
    private val keywordExtractionService: KeywordExtractionService,
    private val documentSearchService: DocumentSearchService,
    private val toolCallbacks: List<ToolCallback>,
    @Value("\${confluence.base-url}") private val confluenceBaseUrl: String
) {

    companion object {
        private const val MAX_SEARCH_RESULTS = 200 // 검색할 최대 문서 수

        private val SYSTEM_PROMPT = """
            You are an AI assistant that answers questions based on Confluence documentation.

            *** CRITICAL: ALL ANSWERS MUST BE IN KOREAN LANGUAGE ***
            *** 모든 답변은 반드시 한글로 작성해야 합니다 ***

            IMPORTANT: In Confluence, any page can have child pages (not just folders).

            Role and Procedures:
            1. Review the search results provided below - they already contain full document content and keywords
            2. Identify relevant documents from the search results
            3. Understand the user's intent from their question
            4. Answer based on the provided content first - only use tools if additional information is needed

            Case 1 - Hierarchical/Comprehensive Query:
            Use 'get_all_child_pages' tool when the question includes:
            - "in", "under", "within", "below", "contents of", "under the"
            - "folder", "page contents"
            - "all", "entire", "work status", "current status"
            Examples: "contents in User Guide", "all documents in Dev Guide", "User Guide work status"

            Procedure:
            1. Find the page ID from search results
            2. Use 'get_all_child_pages' tool to recursively retrieve the page and all child pages
            3. CRITICAL: The tool result includes ALL descendant pages at all depths (children, grandchildren, great-grandchildren, etc.)
            4. CRITICAL: You MUST read and analyze the "Page Content" section of EACH page
            5. Review pages at all levels (Level 0, Level 1, Level 2, etc.)
            6. Synthesize information extracted from each page to provide comprehensive overview
            7. Structure your answer with: key content, progress status, completed work, pending work

            Case 2 - Specific Topic Query:
            For questions about specific topics or keywords:
            - First, analyze the content already provided in search results
            - The search results already contain full document content and keywords
            - Only use 'get_page_by_id' tool if you need to verify or get additional details not in search results
            - Prefer answering directly from search results when sufficient information is available

            Final Answer Guidelines - VERY IMPORTANT:
            1. *** WRITE YOUR ENTIRE ANSWER IN KOREAN ONLY ***
            2. Analyze the content provided in search results first
            3. For work status questions: Structure completed work, in-progress work, and key content
            4. Extract key information and provide meaningful insights
            5. MUST include titles and links of referenced Confluence pages in your Korean answer
            6. Clearly state if answer cannot be found in documentation
            7. Only use tools for hierarchical queries or when search results lack sufficient detail

            Search Results (Full Document Content):
            {context}

            User Question:
            {question}

            *** REMEMBER: Your entire response must be written in Korean language (한글로 답변) ***

            Analyze the search results content and answer the question. Use tools only when necessary for hierarchical exploration or additional verification.
        """.trimIndent()
    }

    fun chat(message: String, keywords: List<String>?): Flux<String> {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        log.info { "[Chat Request] User message: $message" }
        if (keywords != null) {
            log.info { "[Keywords Provided] $keywords" }
        }

        return mono {
            // Extract keywords if not provided
            val searchKeywords = keywords ?: keywordExtractionService.extractKeywords(message)
            log.info { "[Keyword Extraction] $searchKeywords" }

            // Multi-strategy search: keyword-based + content-based + general search
            log.info { "[Document Search] Using multi-strategy search" }

            val allResults = mutableMapOf<String, SearchResult>()

            // Strategy 1: Keyword-based search (if keywords are extracted)
            if (searchKeywords.isNotEmpty()) {
                log.info { "  [Keyword Search] Searching with ${searchKeywords.size} keywords" }
                val keywordQuery = searchKeywords.joinToString(", ")
                val keywordResults = documentSearchService.searchByKeywords(keywordQuery, MAX_SEARCH_RESULTS)
                keywordResults.forEach { result ->
                    allResults[result.id] = result.copy(score = result.score * 1.2) // Boost keyword matches
                }
                log.info { "    Found ${keywordResults.size} results from keyword search" }
            }

            // Strategy 2: Content-based semantic search
            log.info { "  [Content Search] Semantic search on content" }
            val contentResults = documentSearchService.searchByContent(message, MAX_SEARCH_RESULTS)
            contentResults.forEach { result ->
                val existing = allResults[result.id]
                if (existing == null || result.score > existing.score) {
                    allResults[result.id] = result.copy(score = result.score * 1.1) // Boost content matches
                }
            }
            log.info { "    Found ${contentResults.size} results from content search" }

            // Sort by score (descending) and take top results
            val combinedResults = allResults.values
                .sortedByDescending { it.score }
                .take(MAX_SEARCH_RESULTS)

            log.info { "[Combined Results] Total unique documents: ${combinedResults.size} (keyword: ${searchKeywords.size}, strategies: 3)" }
            combinedResults
        }
            .flatMapMany { searchResults ->
                log.info { "[Search Results] Found ${searchResults.size} documents" }
                val context = if (searchResults.isNotEmpty()) {
                    buildString {
                        append("발견된 관련 문서 ${searchResults.size}개 (전체 내용 포함):\n\n")
                        searchResults.forEachIndexed { index, result ->
                            val pageUrl = buildConfluencePageUrl(result.spaceKey, result.id)
                            append("${index + 1}. 제목: ${result.title}\n")
                            append("   페이지 ID: ${result.id}\n")
                            append("   경로: ${result.path}\n")
                            append("   링크: $pageUrl\n")
                            if (result.keywords.isNotBlank()) {
                                append("   키워드: ${result.keywords}\n")
                            }
                            append("   관련도 점수: ${"%.2f".format(result.score)}\n")
                            append("   내용: ${result.content}")
                            append("\n\n")
                        }
                    }
                } else {
                    log.warn { "[No Results] No Confluence pages found" }
                    "관련 Confluence 페이지를 찾을 수 없습니다. 다른 도구를 사용하여 검색을 시도하세요."
                }

                log.info { "[AI Processing] Starting AI chat with ${searchResults.size} document previews" }
                log.info { "Context preview: ${context.take(300)}..." }

                val promptTemplate = PromptTemplate(SYSTEM_PROMPT)
                val prompt = promptTemplate.create(mapOf("context" to context, "question" to message))

                chatClient.prompt(prompt)
                    .toolCallbacks(toolCallbacks)
                    .stream().content()
                    .doOnComplete {
                        log.info { "[Chat Completed] Response stream finished" }
                        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                    }
                    .doOnError { error ->
                        log.error(error) { "[Chat Error] ${error.message}" }
                        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
                    }
            }
    }

    /**
     * Build Confluence page URL from space key and page ID
     */
    private fun buildConfluencePageUrl(spaceKey: String, pageId: String): String {
        // Extract base domain from API URL
        // e.g., https://okestro.atlassian.net/wiki/api/v2 -> https://okestro.atlassian.net/wiki
        val baseDomain = confluenceBaseUrl.substringBefore("/api/v2")

        // Handle chunk IDs (e.g., "12345_chunk_0" -> "12345")
        val actualPageId = if (pageId.contains("_chunk_")) {
            pageId.substringBefore("_chunk_")
        } else {
            pageId
        }

        return "$baseDomain/spaces/$spaceKey/pages/$actualPageId"
    }
}
