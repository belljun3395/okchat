package com.okestro.okchat.ai.tools.confluence

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.GetSpaceByKeyInput
import com.okestro.okchat.ai.model.ToolOutput
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
                    "thought": {
                      "type": "string",
                      "description": "The reasoning for using this tool in this specific context."
                    },
                    "spaceKey": {
                      "type": "string",
                      "description": "The Confluence space key (e.g., 'CBSPPP2411')"
                    }
                  },
                  "required": ["thought", "spaceKey"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, GetSpaceByKeyInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val spaceKey = input.spaceKey

            val spaceId = confluenceService.getSpaceIdByKey(spaceKey)
            val answer = "Successfully found space with key '$spaceKey'. Space ID: $spaceId"

            objectMapper.writeValueAsString(ToolOutput(thought = thought, answer = answer))
        } catch (e: IllegalArgumentException) {
            objectMapper.writeValueAsString(
                ToolOutput(
                    thought = "An error occurred: the space key was not found.",
                    answer = "Error: ${e.message}"
                )
            )
        } catch (e: Exception) {
            objectMapper.writeValueAsString(
                ToolOutput(
                    thought = "An error occurred while retrieving the space.",
                    answer = "Error retrieving space: ${e.message}"
                )
            )
        }
    }
}
