package com.okestro.okchat.confluence.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.ToolOutput
import com.okestro.okchat.ai.tools.ToolExecutor
import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.tools.dto.GetPageByIdInput
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getPageByIdConfluenceTool")
@Description("Get detailed Confluence page information including full content by page ID")
class GetPageByIdConfluenceTool(
    private val confluenceClient: ConfluenceClient,
    private val objectMapper: ObjectMapper
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_page_by_id")
            .description("Get detailed information about a Confluence page by its ID, including the full page content. Use this when you need to read the complete content of a specific page.")
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
                      "description": "The Confluence page ID"
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
            toolName = "GetPageByIdConfluenceTool",
            objectMapper = objectMapper,
            errorThought = "An error occurred while retrieving the Confluence page."
        ) {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, GetPageByIdInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val pageId = input.pageId

            val page = confluenceClient.getPageById(pageId)

            val answer = buildString {
                append("=== Page Information ===\n\n")
                append("Title: ${page.title}\n")
                append("ID: ${page.id}\n")
                append("Status: ${page.status ?: "N/A"}\n")

                if (page.spaceId != null) {
                    append("Space ID: ${page.spaceId}\n")
                }

                if (page.parentId != null) {
                    append("Parent: ${page.parentType}/${page.parentId}\n")
                }

                if (page.version != null) {
                    append("Version: ${page.version.number}\n")
                }

                append("\n=== Page Content ===\n\n")

                val content = page.body?.storage?.value
                if (content != null) {
                    // Strip HTML tags for better readability
                    val cleanContent = content
                        .replace(Regex("<.*?>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    append(cleanContent)
                } else {
                    append("This page has no content.")
                }
            }

            ToolOutput(thought = thought, answer = answer)
        }
    }
}
