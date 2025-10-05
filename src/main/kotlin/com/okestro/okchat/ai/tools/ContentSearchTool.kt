package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.search.strategy.ContentSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("contentSearchTool")
@Description("Search documents by content using semantic vector search")
class ContentSearchTool(
    private val contentSearchStrategy: ContentSearchStrategy,
    private val objectMapper: ObjectMapper
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("search_by_content")
            .description(
                """
                Search documents by content using semantic vector search.
                Focuses on semantic meaning and context within document content.
                Best for: Finding documents based on concepts, ideas, or contextual similarity.
                """.trimIndent()
            )
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "description": "Content query (question or description of what you're looking for)"
                    },
                    "topK": {
                      "type": "integer",
                      "description": "Number of results to return (default: 10, max: 50)",
                      "default": 10
                    }
                  },
                  "required": ["query"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            val input = objectMapper.readValue(toolInput, Map::class.java)
            val query = input["query"] as? String
                ?: return "Invalid input: query parameter is required"
            val topK = ((input["topK"] as? Number)?.toInt() ?: 10).coerceIn(1, 50)

            log.info { "Content search: query='$query', topK=$topK" }

            val results = runBlocking {
                contentSearchStrategy.search(query, topK)
            }

            if (results.isEmpty()) {
                return "No documents found with content matching: '$query'"
            }

            buildString {
                append("Found ${results.size} document(s) by content search:\n\n")

                results.take(topK).forEachIndexed { index, result ->
                    append("${index + 1}. ${result.title}\n")
                    append("   Score: ${"%.4f".format(result.score.value)}\n")
                    if (result.path.isNotBlank()) {
                        append("   Path: ${result.path}\n")
                    }
                    if (result.spaceKey.isNotBlank()) {
                        append("   Space: ${result.spaceKey}\n")
                    }
                    append("   Content: ${result.content.take(200)}")
                    if (result.content.length > 200) append("...")
                    append("\n\n")
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error in content search: ${e.message}" }
            "Error performing content search: ${e.message}"
        }
    }
}
