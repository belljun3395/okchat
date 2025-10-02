package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.confluence.service.ConfluenceService
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getSpaceByKeyConfluenceTool")
@Description("Get Confluence space information by space key")
class GetSpaceByKeyConfluenceTool(
    private val confluenceService: ConfluenceService,
    private val objectMapper: ObjectMapper
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_space_by_key")
            .description("Get Confluence space information including space ID by providing a space key")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "spaceKey": {
                      "type": "string",
                      "description": "The Confluence space key (e.g., 'CBSPPP2411')"
                    }
                  },
                  "required": ["spaceKey"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            val input = objectMapper.readValue(toolInput, Map::class.java)
            val spaceKey = input["spaceKey"] as? String
                ?: return "Invalid input: spaceKey parameter is required"

            val spaceId = confluenceService.getSpaceIdByKey(spaceKey)
            "Successfully found space with key '$spaceKey'. Space ID: $spaceId"
        } catch (e: IllegalArgumentException) {
            "Error: ${e.message}"
        } catch (e: Exception) {
            "Error retrieving space: ${e.message}"
        }
    }
}
