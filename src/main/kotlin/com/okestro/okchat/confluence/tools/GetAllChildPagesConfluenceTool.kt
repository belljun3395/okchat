package com.okestro.okchat.confluence.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.ToolOutput
import com.okestro.okchat.ai.tools.ToolExecutor
import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.client.Page
import com.okestro.okchat.confluence.tools.dto.GetAllChildPagesInput
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
                    "thought": {
                      "type": "string",
                      "description": "The reasoning for using this tool in this specific context."
                    },
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
                  "required": ["thought", "pageId"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return ToolExecutor.execute(
            toolName = "GetAllChildPagesConfluenceTool",
            objectMapper = objectMapper,
            errorThought = "An error occurred while getting child pages."
        ) {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, GetAllChildPagesInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val pageId = input.pageId
            val maxDepth = input.getValidatedMaxDepth()

            log.info { "[ConfluenceTool] Getting all child pages: page_id=$pageId, max_depth=$maxDepth" }

            // Get parent page info first
            val parentPage = confluenceClient.getPageById(pageId)

            // Get all children recursively
            val allPages = mutableListOf<Page>()
            allPages.add(parentPage)
            collectChildrenRecursively(pageId, allPages, 0, maxDepth)

            log.info { "[ConfluenceTool] Found pages: total_count=${allPages.size}, including_parent=true" }

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

            val answer = buildString {
                append("=== ${parentPage.title} - Complete Hierarchy and Content ===\n\n")
                append("IMPORTANT: This result includes all child pages retrieved recursively.\n")
                append("Total ${allPages.size} pages (including top-level page, all depths of child pages)\n\n")

                // Pages count by depth statistics
                val depthStats = depthMap.values.groupingBy { it }.eachCount()
                append("Pages by depth level:\n")
                depthStats.toSortedMap().forEach { (depth, count) ->
                    append("  - Level $depth: $count pages\n")
                }
                append("\n========================================\n\n")

                allPages.sortedBy { depthMap[it.id] ?: 0 }.forEachIndexed { index, page ->
                    val depth = depthMap[page.id] ?: 0
                    val indent = "  ".repeat(depth)

                    append("## ${index + 1}. ${indent}${"└─ ".repeat(minOf(depth, 1))}${page.title}\n")
                    append("$indent- **Page ID**: ${page.id}\n")
                    append("$indent- **Depth Level**: Level $depth\n")
                    if (page.parentId != null) {
                        val parentTitle = pageMap[page.parentId]?.title ?: "Unknown"
                        append("$indent- **Parent Page**: $parentTitle (ID: ${page.parentId})\n")
                    }
                    append("$indent- **Status**: ${page.status ?: "N/A"}\n")
                    if (page.version != null) {
                        append("$indent- **Version**: ${page.version.number}\n")
                    }

                    append("\n$indent### Page Content:\n")
                    val content = page.body?.storage?.value
                    if (content != null) {
                        val cleanContent = stripHtml(content)
                        if (cleanContent.isNotBlank()) {
                            val contentLines = cleanContent.take(3000).lines()
                            contentLines.forEach { line ->
                                append("${indent}$line\n")
                            }
                            if (cleanContent.length > 3000) {
                                append("\n$indent... (Content truncated to 3000 characters)\n")
                            }
                        } else {
                            append("$indent(No content)\n")
                        }
                    } else {
                        append("$indent(No content)\n")
                    }
                    append("\n")
                    append("========================================\n\n")
                }

                append("\nVerified all ${allPages.size} pages content.\n")
                append("IMPORTANT: This is not just a simple list. The actual content of all pages above is included.\n")
                append("Please be sure to read and analyze the 'Page Content' section of each page to comprehensively summarize the overall work status and current situation.\n")
                append("Don't just list page titles, but extract and answer the main content covered on each page, completed tasks, ongoing tasks, etc.")
            }

            ToolOutput(thought = thought, answer = answer)
        }
    }

    private fun collectChildrenRecursively(
        pageId: String,
        allPages: MutableList<Page>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth >= maxDepth) {
            log.warn { "[ConfluenceTool] Reached max depth: depth=$maxDepth, page_id=$pageId" }
            return
        }

        try {
            log.debug { "[ConfluenceTool] Collecting children: depth=$currentDepth, page_id=$pageId" }
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
                    log.debug { "[ConfluenceTool] Recursively checking children: title='${childPage.title}', id=${childPage.id}" }
                    collectChildrenRecursively(childPage.id, allPages, currentDepth + 1, maxDepth)
                }

                cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            } while (cursor != null)

            log.debug { "[ConfluenceTool] Completed depth traversal: depth=$currentDepth, page_id=$pageId, children_count=$totalChildren" }
        } catch (e: Exception) {
            log.warn(e) { "[ConfluenceTool] Failed to get children: page_id=$pageId, depth=$currentDepth, error=${e.message}" }
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<.*?>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
