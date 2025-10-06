package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
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
        return try {
            val input = objectMapper.readValue(toolInput, Map::class.java)
            val thought = input["thought"] as? String ?: "No thought provided."
            val folderId = input["folderId"] as? String
                ?: return "Invalid input: folderId parameter is required"

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

            objectMapper.writeValueAsString(mapOf("thought" to thought, "answer" to answer))
        } catch (e: Exception) {
            objectMapper.writeValueAsString(mapOf("thought" to "An error occurred while retrieving the folder.", "answer" to "Error retrieving folder: ${e.message}"))
        }
    }
}
