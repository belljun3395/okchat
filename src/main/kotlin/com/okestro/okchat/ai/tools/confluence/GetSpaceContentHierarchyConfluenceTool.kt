package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.service.ConfluenceService
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getSpaceContentHierarchyConfluenceTool")
@Description("Get hierarchical structure of all content (pages and folders) in a Confluence space")
class GetSpaceContentHierarchyConfluenceTool(
    private val confluenceService: ConfluenceService,
    private val objectMapper: ObjectMapper
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_space_content_hierarchy")
            .description("Get the hierarchical structure of all content (pages and folders) in a Confluence space. Returns a tree structure showing the organization of content.")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "spaceId": {
                      "type": "string",
                      "description": "The Confluence space ID"
                    },
                    "maxDepth": {
                      "type": "integer",
                      "description": "Maximum depth to show in hierarchy (default: 3)",
                      "default": 3
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
            val maxDepth = (input["maxDepth"] as? Number)?.toInt() ?: 3

            val hierarchy = confluenceService.getSpaceContentHierarchy(spaceId)

            buildString {
                append("Content Hierarchy for Space $spaceId:\n")
                append("Total Items: ${hierarchy.getTotalCount()} ")
                append("(Folders: ${hierarchy.getAllFolders().size}, Pages: ${hierarchy.getAllPages().size})\n")
                append("Max Depth: ${hierarchy.getMaxDepth()}\n\n")

                hierarchy.rootNodes.forEach { node ->
                    appendNodeTree(node, "", true, 0, maxDepth)
                }
            }
        } catch (e: Exception) {
            "Error retrieving space hierarchy: ${e.message}"
        }
    }

    private fun StringBuilder.appendNodeTree(
        node: ContentNode,
        prefix: String,
        isLast: Boolean,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth >= maxDepth) {
            return
        }

        val connector = if (isLast) "└── " else "├── "
        val icon = if (node.type.name == "FOLDER") "📁" else "📄"
        append("$prefix$connector$icon ${node.title} (ID: ${node.id})\n")

        if (node.children.isNotEmpty()) {
            val newPrefix = prefix + if (isLast) "    " else "│   "
            node.children.forEachIndexed { index, child ->
                appendNodeTree(child, newPrefix, index == node.children.lastIndex, currentDepth + 1, maxDepth)
            }
        }
    }
}
