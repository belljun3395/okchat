package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.GetFolderByIdInput
import com.okestro.okchat.ai.model.ToolOutput
import com.okestro.okchat.ai.tools.ToolExecutor
import com.okestro.okchat.confluence.client.ConfluenceClient
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getFolderByIdConfluenceTool")
@Description("Get Confluence folder information by folder ID")
class GetFolderByIdConfluenceTool(
    private val confluenceClient: ConfluenceClient,
    private val objectMapper: ObjectMapper
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_folder_by_id")
            .description("Get Confluence folder information by providing the folder ID")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "thought": {
                      "type": "string",
                      "description": "The reasoning for using this tool in this specific context."
                    },
                    "folderId": {
                      "type": "string",
                      "description": "The Confluence folder ID"
                    }
                  },
                  "required": ["thought", "folderId"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return ToolExecutor.execute(
            toolName = "GetFolderByIdConfluenceTool",
            objectMapper = objectMapper,
            errorThought = "An error occurred while retrieving the Confluence folder."
        ) {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, GetFolderByIdInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val folderId = input.folderId

            val folder = confluenceClient.getFolderById(folderId)
            val answer = buildString {
                append("Folder Information:\n")
                append("- Title: ${folder.title}\n")
                append("- ID: ${folder.id}\n")
                append("- Status: ${folder.status}\n")
                if (folder.parentId != null) {
                    append("- Parent: ${folder.parentType}/${folder.parentId}\n")
                }
                append("- Version: ${folder.version?.number ?: "N/A"}\n")
            }

            ToolOutput(thought = thought, answer = answer)
        }
    }
}
