package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component

@Component("searchDocumentsTool")
@Description("Search documents in OpenSearch vector store using keyword search")
class SearchDocumentsTool(
    private val openSearchClient: OpenSearchClient,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("search_documents")
            .description("Search documents using keyword-based full-text search. Use this to find Confluence pages by specific keywords or terms.")
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
                      "description": "Search query keywords"
                    },
                    "limit": {
                      "type": "integer",
                      "description": "Number of results to return (default: 5, max: 20)",
                      "default": 5
                    },
                    "filterBySpace": {
                      "type": "string",
                      "description": "Optional: Filter by Confluence space key (e.g., 'CBSPPP2411')"
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
            @Suppress("UNCHECKED_CAST")
            val input = objectMapper.readValue(toolInput, Map::class.java) as Map<String, Any>
            val thought = input["thought"] as? String ?: "No thought provided."
            val query = input["query"] as? String
                ?: return "Invalid input: query parameter is required"
            val limit = ((input["limit"] as? Number)?.toInt() ?: 5).coerceIn(1, 20)
            val filterBySpace = input["filterBySpace"] as? String

            log.info { "Searching OpenSearch: query='$query', limit=$limit, space=$filterBySpace" }

            val searchResponse = openSearchClient.search({ s ->
                s.index(indexName)
                    .size(limit)
                    .query { q ->
                        if (!filterBySpace.isNullOrBlank()) {
                            // Add space filter
                            q.bool { b ->
                                b.must { m ->
                                    m.multiMatch { mm ->
                                        mm.query(query)
                                            .fields(listOf("content", "metadata.title"))
                                    }
                                }
                                    .filter { f ->
                                        f.term { t ->
                                            t.field("metadata.spaceKey")
                                                .value(org.opensearch.client.opensearch._types.FieldValue.of(filterBySpace))
                                        }
                                    }
                            }
                        } else {
                            // No filter
                            q.multiMatch { mm ->
                                mm.query(query)
                                    .fields(listOf("content", "metadata.title"))
                            }
                        }
                    }
            }, Map::class.java)

            val hits = searchResponse.hits().hits()

            val answer = if (hits.isEmpty()) {
                "No documents found for query: '$query'"
            } else {
                buildString {
                    append("Found ${hits.size} document(s):\n\n")

                    hits.forEachIndexed { index, hit ->
                        val doc = hit.source() ?: return@forEachIndexed

                        // Extract metadata - support both flat and nested structure
                        @Suppress("UNCHECKED_CAST")
                        val metadata = doc["metadata"] as? Map<String, Any> ?: emptyMap()

                        val title = doc["metadata.title"]?.toString() ?: metadata["title"]?.toString() ?: "Untitled"
                        val path = doc["metadata.path"]?.toString() ?: metadata["path"]?.toString() ?: ""
                        val spaceKey = doc["metadata.spaceKey"]?.toString() ?: metadata["spaceKey"]?.toString() ?: ""
                        val pageId = doc["metadata.id"]?.toString() ?: metadata["id"]?.toString() ?: doc["id"]?.toString() ?: ""
                        val content = doc["content"]?.toString() ?: ""

                        append("${index + 1}. $title\n")
                        if (path.isNotBlank()) {
                            append("   Path: $path\n")
                        }
                        if (spaceKey.isNotBlank() && pageId.isNotBlank()) {
                            append("   Page ID: $pageId\n")
                            append("   Space: $spaceKey\n")
                        }
                        append("   Content Preview: ${content.take(200)}")
                        if (content.length > 200) append("...")
                        append("\n\n")
                    }
                }
            }

            objectMapper.writeValueAsString(mapOf("thought" to thought, "answer" to answer))
        } catch (e: Exception) {
            log.error(e) { "Error searching OpenSearch: ${e.message}" }
            objectMapper.writeValueAsString(mapOf("thought" to "An error occurred during the document search.", "answer" to "Error searching documents: ${e.message}"))
        }
    }
}
