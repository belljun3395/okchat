package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.confluence.service.ContentCollector
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getPagesBySpaceIdConfluenceTool")
@Description("Get all pages in a Confluence space by space ID")
class GetPagesBySpaceIdConfluenceTool(
    private val contentCollector: ContentCollector,
    private val objectMapper: ObjectMapper
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_pages_by_space_id")
            .description("Get all pages in a Confluence space by providing the space ID. Returns a list of page titles and IDs.")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "spaceId": {
                      "type": "string",
                      "description": "The Confluence space ID"
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Maximum number of pages to return (default: 20)",
                      "default": 20
                    }
                  },
                  "required": ["spaceId"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            val input = objectMapper.readValue(toolInput, Map::class.java)
            val spaceId = input["spaceId"] as? String
                ?: return "Invalid input: spaceId parameter is required"
            val limit = (input["limit"] as? Number)?.toInt() ?: 20

            val allPages = contentCollector.collectAllContent(spaceId)
            val limitedPages = allPages.take(limit)

            if (limitedPages.isEmpty()) {
                "No pages found in space with ID: $spaceId"
            } else {
                buildString {
                    append("Found ${allPages.size} total pages in space (showing first $limit):\n\n")
                    limitedPages.forEach { page ->
                        append("- ${page.title} (ID: ${page.id})")
                        if (page.parentId != null) {
                            append(" [Parent: ${page.parentType}/${page.parentId}]")
                        }
                        append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            "Error retrieving pages: ${e.message}"
        }
    }
}
