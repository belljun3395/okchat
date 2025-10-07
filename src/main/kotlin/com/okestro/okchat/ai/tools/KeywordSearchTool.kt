package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.SearchByKeywordInput
import com.okestro.okchat.ai.model.ToolOutput
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.strategy.KeywordSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("keywordSearchTool")
@Description("Search documents using keyword-based hybrid search (text + vector)")
class KeywordSearchTool(
    private val keywordSearchStrategy: KeywordSearchStrategy,
    private val objectMapper: ObjectMapper
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("search_by_keyword")
            .description(
                """
                Search documents using keyword-based hybrid search.
                Combines exact keyword matching with semantic vector search.
                Best for: Finding documents with specific terms or technical keywords.
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
                    "keywords": {
                      "type": "string",
                      "description": "Keywords to search for (space-separated)"
                    },
                    "topK": {
                      "type": "integer",
                      "description": "Number of results to return (default: 10, max: 50)",
                      "default": 10
                    }
                  },
                  "required": ["thought", "keywords"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, SearchByKeywordInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val keywords = input.keywords
            val topK = input.getValidatedTopK()

            log.info { "Keyword search: keywords='$keywords', topK=$topK" }

            // Convert to SearchCriteria
            val criteria = SearchKeywords.fromStrings(keywords.split(",").map { it.trim() })

            val results = runBlocking {
                keywordSearchStrategy.search(criteria, topK)
            }

            val answer = if (results.isEmpty()) {
                "No documents found for keywords: '$keywords'"
            } else {
                buildString {
                    append("Found ${results.size} document(s) by keyword search:\n\n")

                    results.take(topK).forEachIndexed { index, result ->
                        append("${index + 1}. ${result.title}\n")
                        append("   Score: ${"%.4f".format(result.score.value)}\n")
                        if (result.path.isNotBlank()) {
                            append("   Path: ${result.path}\n")
                        }
                        if (result.spaceKey.isNotBlank()) {
                            append("   Space: ${result.spaceKey}\n")
                        }
                        if (result.keywords.isNotBlank()) {
                            append("   Keywords: ${result.keywords}\n")
                        }
                        append("   Content: ${result.content.take(200)}")
                        if (result.content.length > 200) append("...")
                        append("\n\n")
                    }
                }
            }

            objectMapper.writeValueAsString(ToolOutput(thought = thought, answer = answer))
        } catch (e: Exception) {
            log.error(e) { "Error in keyword search: ${e.message}" }
            objectMapper.writeValueAsString(
                ToolOutput(
                    thought = "An error occurred during the keyword search.",
                    answer = "Error performing keyword search: ${e.message}"
                )
            )
        }
    }
}
