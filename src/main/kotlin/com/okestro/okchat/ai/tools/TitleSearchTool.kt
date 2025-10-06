package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.search.strategy.TitleSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("titleSearchTool")
@Description("Search documents by title using hybrid search (text + vector)")
class TitleSearchTool(
    private val titleSearchStrategy: TitleSearchStrategy,
    private val objectMapper: ObjectMapper
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("search_by_title")
            .description(
                """
                Search documents by title using hybrid search.
                Prioritizes matching document titles over content.
                Best for: Finding documents by name or when you know the approximate title.
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
                    "query": {
                      "type": "string",
                      "description": "Title query (full or partial title)"
                    },
                    "topK": {
                      "type": "integer",
                      "description": "Number of results to return (default: 10, max: 50)",
                      "default": 10
                    }
                  },
                  "required": ["thought", "query"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            val input = objectMapper.readValue(toolInput, Map::class.java)
            val thought = input["thought"] as? String ?: "No thought provided."
            val query = input["query"] as? String
                ?: return "Invalid input: query parameter is required"
            val topK = ((input["topK"] as? Number)?.toInt() ?: 10).coerceIn(1, 50)

            log.info { "Title search: query='$query', topK=$topK" }

            val results = runBlocking {
                titleSearchStrategy.search(query, topK)
            }

            val answer = if (results.isEmpty()) {
                "No documents found with title matching: '$query'"
            } else {
                buildString {
                    append("Found ${results.size} document(s) by title search:\n\n")

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
            }

            objectMapper.writeValueAsString(mapOf("thought" to thought, "answer" to answer))
        } catch (e: Exception) {
            log.error(e) { "Error in title search: ${e.message}" }
            objectMapper.writeValueAsString(mapOf("thought" to "An error occurred during the title search.", "answer" to "Error performing title search: ${e.message}"))
        }
    }
}
