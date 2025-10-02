package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component
import org.typesense.api.Client

@Component("getSchemaInfoTool")
@Description("Get information about the  collection schema and document structure")
class GetDocumentSchemaInfoTool(
    private val typesenseClient: Client,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("get_document_schema_info")
            .description("Get schema information about the  collection including field names and types. Useful for debugging and understanding the data structure.")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {},
                  "required": []
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return try {
            log.info { "Retrieving schema for collection: $collectionName" }

            // Get collection schema
            val collection = typesenseClient.collections(collectionName).retrieve()

            buildString {
                append("Collection: ${collection.name}\n")
                append("Number of documents: ${collection.numDocuments}\n\n")
                append("Schema Fields:\n")

                collection.fields?.forEach { field ->
                    append("- ${field.name}: ${field.type}\n")
                }

                // Get a sample document to show actual structure
                append("\nSample Document Structure:\n")
                if (collection.numDocuments > 0) {
                    try {
                        // Export documents (limited to first few lines)
                        val exportResult = typesenseClient.collections(collectionName)
                            .documents()
                            .export()

                        if (exportResult.isNotBlank()) {
                            // Parse the first line (JSONL format - each line is a JSON document)
                            val firstLine = exportResult.lines().firstOrNull { it.isNotBlank() }
                            if (firstLine != null) {
                                val sampleDoc = objectMapper.readValue(firstLine, Map::class.java)
                                append(
                                    objectMapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(sampleDoc)
                                )
                            } else {
                                append("No documents found in collection.")
                            }
                        } else {
                            append("No documents found in collection.")
                        }
                    } catch (e: Exception) {
                        log.warn(e) { "Could not retrieve sample document: ${e.message}" }
                        append("Could not retrieve sample document.\n")
                        append("Error: ${e.message}")
                    }
                } else {
                    append("No documents in collection.")
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error retrieving schema: ${e.message}" }
            "Error retrieving schema information: ${e.message}"
        }
    }
}
