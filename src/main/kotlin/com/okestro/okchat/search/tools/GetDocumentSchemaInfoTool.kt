package com.okestro.okchat.search.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.dto.ToolOutput
import com.okestro.okchat.ai.tools.ToolExecutor
import com.okestro.okchat.search.tools.dto.GetDocumentSchemaInput
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("getSchemaInfoTool")
@Description("Get information about the OpenSearch collection schema and document structure")
class GetDocumentSchemaInfoTool(
    private val openSearchClient: OpenSearchClient,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_document_schema_info")
            .description("Get schema information about the OpenSearch collection including field names and types. Useful for debugging and understanding the data structure.")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "thought": {
                      "type": "string",
                      "description": "The reasoning for using this tool in this specific context."
                    }
                  },
                  "required": ["thought"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return ToolExecutor.execute(
            toolName = "GetDocumentSchemaInfo",
            objectMapper = objectMapper,
            errorThought = "An error occurred while retrieving the schema."
        ) {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, GetDocumentSchemaInput::class.java)
            val thought = input.thought ?: "No thought provided."

            log.info { "Retrieving schema for index: $indexName" }

            val answer = retrieveSchemaInfo()

            ToolOutput(thought = thought, answer = answer)
        }
    }

    /**
     * Retrieve schema information including mappings and sample document.
     * Separated for better testability and readability.
     */
    private fun retrieveSchemaInfo(): String {
        // Get index mapping
        val indexResponse = openSearchClient.indices().get { g ->
            g.index(indexName)
        }

        val index = indexResponse.get(indexName)
        val mappings = index?.mappings()

        return buildString {
            append("Index: $indexName\n")

            // Get document count
            appendDocumentCount()

            append("Schema Fields:\n")
            mappings?.properties()?.forEach { (name, property) ->
                append("- $name: ${property._kind()}\n")
            }

            // Get a sample document
            append("\nSample Document Structure:\n")
            appendSampleDocument()
        }
    }

    private fun StringBuilder.appendDocumentCount() {
        try {
            val stats = openSearchClient.indices().stats { s ->
                s.index(indexName)
            }
            val docCount = stats.indices()?.get(indexName)?.primaries()?.docs()?.count() ?: 0
            append("Number of documents: $docCount\n\n")
        } catch (_: Exception) {
            append("Number of documents: Unknown\n\n")
        }
    }

    private fun StringBuilder.appendSampleDocument() {
        try {
            val searchResponse = openSearchClient.search({ s ->
                s.index(indexName)
                    .size(1)
                    .query { q -> q.matchAll { it } }
            }, Map::class.java)

            val hits = searchResponse.hits().hits()
            if (hits.isNotEmpty()) {
                val sampleDoc = hits.first().source()
                append(
                    objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(sampleDoc)
                )
            } else {
                append("No documents found in index.")
            }
        } catch (e: Exception) {
            log.warn(e) { "Could not retrieve sample document: ${e.message}" }
            append("Could not retrieve sample document.\n")
            append("Error: ${e.message}")
        }
    }
}
