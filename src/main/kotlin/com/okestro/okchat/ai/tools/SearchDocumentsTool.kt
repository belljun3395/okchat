package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component
import org.typesense.api.Client
import org.typesense.model.SearchParameters

@Component("searchDocumentsTool")
@Description("Search documents in  vector store using keyword search")
class SearchDocumentsTool(
    private val typesenseClient: Client,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("search_documents")
            .description("Search documents in using keyword-based full-text search. Use this to find Confluence pages by specific keywords or terms.")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
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
            val limit = ((input["limit"] as? Number)?.toInt() ?: 5).coerceIn(1, 20)
            val filterBySpace = input["filterBySpace"] as? String

            log.info { "Searching : query='$query', limit=$limit, space=$filterBySpace" }

            val searchParameters = SearchParameters()
                .q(query)
                .queryBy("content,metadata.title") // Search in both content and title
                .perPage(limit)
                .page(1)

            // Add space filter if provided
            if (!filterBySpace.isNullOrBlank()) {
                searchParameters.filterBy("metadata.spaceKey:=$filterBySpace")
            }

            val searchResult = typesenseClient.collections(collectionName)
                .documents()
                .search(searchParameters)

            val hits = searchResult.hits ?: emptyList()

            if (hits.isEmpty()) {
                return "No documents found for query: '$query'"
            }

            buildString {
                append("Found ${hits.size} document(s):\n\n")

                hits.forEachIndexed { index, hit ->
                    val doc = hit.document ?: return@forEachIndexed

                    // Extract metadata (Spring AI stores it as a nested object)
                    val metadata = doc["metadata"] as? Map<*, *>
                    val title = metadata?.get("title")?.toString() ?: "Untitled"
                    val path = metadata?.get("path")?.toString() ?: ""
                    val spaceKey = metadata?.get("spaceKey")?.toString() ?: ""
                    val pageId = metadata?.get("id")?.toString() ?: doc["id"]?.toString() ?: ""
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
        } catch (e: Exception) {
            log.error(e) { "Error searching : ${e.message}" }
            "Error searching documents: ${e.message}"
        }
    }
}
