package com.okestro.okchat.search.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.dto.ToolOutput
import com.okestro.okchat.ai.tools.ToolExecutor
import com.okestro.okchat.search.model.SearchDocument
import com.okestro.okchat.search.support.MetadataFields
import com.okestro.okchat.search.tools.dto.SearchDocumentsInput
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
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
        return ToolExecutor.execute(
            toolName = "SearchDocumentsTool",
            objectMapper = objectMapper,
            errorThought = "An error occurred while searching documents."
        ) {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, SearchDocumentsInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val query = input.query
            val limit = input.getValidatedLimit()
            val filterBySpace = input.filterBySpace

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
                                            .fields(listOf("content", MetadataFields.TITLE))
                                    }
                                }
                                    .filter { f ->
                                        f.term { t ->
                                            t.field(MetadataFields.SPACE_KEY)
                                                .value(FieldValue.of(filterBySpace))
                                        }
                                    }
                            }
                        } else {
                            // No filter
                            q.multiMatch { mm ->
                                mm.query(query)
                                    .fields(listOf("content", MetadataFields.TITLE))
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
                        val source = hit.source() ?: return@forEachIndexed

                        // Convert to type-safe SearchDocument
                        // fromMap now handles Any type safely
                        val doc = SearchDocument.fromMap(source)

                        val title = doc.getTitle()
                        val path = doc.getPath()
                        val spaceKey = doc.getSpaceKey()
                        val pageId = doc.resolveMetadataId()
                        val content = doc.content

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

            ToolOutput(thought = thought, answer = answer)
        }
    }
}
