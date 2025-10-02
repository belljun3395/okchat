package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.client.Page
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getAllChildPagesConfluenceTool")
@Description("Get all child pages under any Confluence page recursively with full content")
class GetAllChildPagesConfluenceTool(
    private val confluenceClient: ConfluenceClient,
    private val objectMapper: ObjectMapper
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_all_child_pages")
            .description(
                """
                RECURSIVELY get ALL descendant pages under a Confluence page.

                IMPORTANT: This tool doesn't just get immediate children - it gets ALL descendants:
                - Direct children (Level 1)
                - Grandchildren (Level 2)
                - Great-grandchildren (Level 3)
                - And so on, until no more children exist or maxDepth is reached

                For each page found, the FULL CONTENT is included (not just metadata).

                In Confluence, any page can have child pages (not just folders).

                Use this when:
                - User asks about content "in", "under", "within" a page
                - User wants to know "작업 상황" (work status) of a section
                - Exploring complete hierarchical content
                """.trimIndent()
            )
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "pageId": {
                      "type": "string",
                      "description": "The parent page/folder ID to get ALL descendants from"
                    },
                    "maxDepth": {
                      "type": "integer",
                      "description": "Maximum depth to traverse recursively (default: 10)",
                      "default": 10
                    }
                  },
                  "required": ["pageId"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            val input = objectMapper.readValue(toolInput, Map::class.java)
            val pageId = input["pageId"] as? String
                ?: return "Invalid input: pageId parameter is required"
            val maxDepth = ((input["maxDepth"] as? Number)?.toInt() ?: 10).coerceIn(1, 20)

            log.info { "Getting all child pages for page ID: $pageId (maxDepth: $maxDepth)" }

            // Get parent page info first
            val parentPage = confluenceClient.getPageById(pageId)

            // Get all children recursively
            val allPages = mutableListOf<Page>()
            allPages.add(parentPage)
            collectChildrenRecursively(pageId, allPages, 0, maxDepth)

            log.info { "Found ${allPages.size} pages total (including parent)" }

            // Build hierarchy map for depth calculation
            val pageMap = allPages.associateBy { it.id }
            val depthMap = mutableMapOf<String, Int>()

            fun calculateDepth(pageId: String): Int {
                if (depthMap.containsKey(pageId)) return depthMap[pageId]!!
                val page = pageMap[pageId]
                val depth = if (page?.parentId == null || page.parentId !in pageMap) {
                    0
                } else {
                    calculateDepth(page.parentId) + 1
                }
                depthMap[pageId] = depth
                return depth
            }

            allPages.forEach { calculateDepth(it.id) }

            buildString {
                append("=== 📂 ${parentPage.title} - 전체 계층 구조 및 내용 ===\n\n")
                append("⚠️ 중요: 이 결과는 모든 하위 페이지를 재귀적으로 조회한 것입니다.\n")
                append("총 ${allPages.size}개의 페이지(최상위 페이지 포함, 모든 깊이의 하위 페이지 포함)\n\n")

                // 계층별 페이지 수 통계
                val depthStats = depthMap.values.groupingBy { it }.eachCount()
                append("📊 깊이별 페이지 수:\n")
                depthStats.toSortedMap().forEach { (depth, count) ->
                    append("  - Level $depth: ${count}개\n")
                }
                append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

                allPages.sortedBy { depthMap[it.id] ?: 0 }.forEachIndexed { index, page ->
                    val depth = depthMap[page.id] ?: 0
                    val indent = "  ".repeat(depth)

                    append("## ${index + 1}. ${indent}${"└─ ".repeat(minOf(depth, 1))}${page.title}\n")
                    append("$indent- **페이지 ID**: ${page.id}\n")
                    append("$indent- **계층 깊이**: Level $depth\n")
                    if (page.parentId != null) {
                        val parentTitle = pageMap[page.parentId]?.title ?: "Unknown"
                        append("$indent- **부모 페이지**: $parentTitle (ID: ${page.parentId})\n")
                    }
                    append("$indent- **상태**: ${page.status ?: "N/A"}\n")
                    if (page.version != null) {
                        append("$indent- **버전**: ${page.version.number}\n")
                    }

                    append("\n$indent### 📄 페이지 내용:\n")
                    val content = page.body?.storage?.value
                    if (content != null) {
                        val cleanContent = stripHtml(content)
                        if (cleanContent.isNotBlank()) {
                            val contentLines = cleanContent.take(3000).lines()
                            contentLines.forEach { line ->
                                append("${indent}$line\n")
                            }
                            if (cleanContent.length > 3000) {
                                append("\n$indent... (내용이 길어 3000자로 제한됨)\n")
                            }
                        } else {
                            append("$indent(내용 없음)\n")
                        }
                    } else {
                        append("$indent(내용 없음)\n")
                    }
                    append("\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
                }

                append("\n✅ 총 ${allPages.size}개의 페이지 내용을 모두 확인했습니다.\n")
                append("⚠️ 이것은 단순한 목록이 아닙니다. 위의 모든 페이지의 실제 내용이 포함되어 있습니다.\n")
                append("각 페이지의 '페이지 내용' 섹션을 반드시 읽고 분석하여 전체적인 작업 상황과 현황을 종합적으로 정리해주세요.\n")
                append("단순히 페이지 제목만 나열하지 말고, 각 페이지에서 다루는 주요 내용, 완료된 작업, 진행 중인 작업 등을 추출하여 답변하세요.")
            }
        } catch (e: Exception) {
            log.error(e) { "Error getting child pages: ${e.message}" }
            "Error retrieving child pages: ${e.message}"
        }
    }

    private fun collectChildrenRecursively(
        pageId: String,
        allPages: MutableList<Page>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth >= maxDepth) {
            log.warn { "⚠️ Reached max depth $maxDepth for page $pageId" }
            return
        }

        try {
            log.debug { "📂 Collecting children at depth $currentDepth for page $pageId" }
            var cursor: String? = null
            var totalChildren = 0

            do {
                val response = confluenceClient.getPageChildren(pageId, cursor)
                val currentBatchSize = response.results.size
                totalChildren += currentBatchSize

                log.debug { "  Found $currentBatchSize children in this batch (total: $totalChildren so far)" }

                allPages.addAll(response.results)

                // Recursively get children of each child - THIS IS KEY!
                // Each child page might have its own children, so we need to check all of them
                response.results.forEach { childPage ->
                    log.debug { "  🔄 Recursively checking children of: ${childPage.title} (ID: ${childPage.id})" }
                    collectChildrenRecursively(childPage.id, allPages, currentDepth + 1, maxDepth)
                }

                cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            } while (cursor != null)

            log.debug { "✅ Completed depth $currentDepth for page $pageId: found $totalChildren direct children" }
        } catch (e: Exception) {
            log.warn(e) { "❌ Failed to get children for page $pageId at depth $currentDepth: ${e.message}" }
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<.*?>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
