package com.okestro.okchat.ai.tools.document

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.SearchByQueryInput
import com.okestro.okchat.ai.model.ToolOutput
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.strategy.ContentSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("searchDocumentContentTool")
@Description("Search documents by content using semantic vector search")
class SearchDocumentContentTool(
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
                    "thought": {
                      "type": "string",
                      "description": "The reasoning for using this tool in this specific context."
                    },
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
                  "required": ["thought", "query"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, SearchByQueryInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val query = input.query
            val topK = input.getValidatedTopK()

            log.info { "Content search: query='$query', topK=$topK" }

            // Convert to SearchCriteria
            val criteria = SearchContents.fromStrings(listOf(query))

            val results = runBlocking {
                contentSearchStrategy.search(criteria, topK)
            }

            val answer = if (results.isEmpty()) {
                "No documents found with content matching: '$query'"
            } else {
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
            }

            objectMapper.writeValueAsString(ToolOutput(thought = thought, answer = answer))
        } catch (e: Exception) {
            log.error(e) { "Error in content search: ${e.message}" }
            objectMapper.writeValueAsString(
                ToolOutput(
                    thought = "An error occurred during the content search.",
                    answer = "Error performing content search: ${e.message}"
                )
            )
        }
    }
}
