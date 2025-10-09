package com.okestro.okchat.search.service

import com.okestro.okchat.search.model.Document
import com.okestro.okchat.search.model.DocumentNode
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.strategy.MultiSearchStrategy
import com.okestro.okchat.search.util.extractChunk
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.collections.get

private val log = KotlinLogging.logger {}

/**
 * Document search service that delegates to a pluggable search strategy.
 *
 * Responsibility:
 * - Provide a stable API for multi-search operations
 * - Delegate actual search execution to a configurable strategy
 * - Log search operations at the service layer
 *
 * Benefits of Strategy pattern:
 * - Flexibility: Easy to switch between different search implementations
 *   (e.g., HybridSearch, SequentialSearch, ParallelSearch)
 * - Testability: Strategies can be tested independently
 * - Maintainability: Search logic is isolated in strategy classes
 * - Clarity: Service has single responsibility (delegation), strategy has single responsibility (execution)
 */
@Service
class DocumentSearchService(
    private val searchStrategy: MultiSearchStrategy,
    private val openSearchClient: OpenSearchClient,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) {

    /**
     * Perform multi-search across titles, contents, paths, and keywords.
     * Delegates to the configured search strategy.
     *
     * Uses polymorphic SearchCriteria for flexibility:
     * - Adding new search types doesn't require changing this method
     * - Each criteria type knows how to convert itself to a query
     *
     * @param titles Title search terms
     * @param contents Content search terms
     * @param paths Path search terms
     * @param keywords Keyword search terms
     * @param topK Maximum results per search type
     * @return Combined multi-search results
     */
    suspend fun multiSearch(
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        keywords: SearchKeywords?,
        topK: Int = 50
    ): MultiSearchResult {
        log.info { "[DocumentSearchService] Delegating to ${searchStrategy.getStrategyName()} strategy" }

        // Build criteria list (polymorphic approach)
        val criteria = listOfNotNull(keywords, titles, contents, paths)

        return searchStrategy.search(
            searchCriteria = criteria,
            topK = topK
        )
    }

    /**
     * Get all document IDs under a specific path from OpenSearch
     * This is useful for bulk permission operations
     *
     * @param documentPath The path to search for (e.g., "팀회의 > 2025")
     * @return List of document IDs that match or are under the specified path
     */
    suspend fun searchAllByPath(documentPath: String): List<Document> = withContext(Dispatchers.IO) {
        val documents = mutableMapOf<String, Document>() // Use map to handle chunks and keep unique docs

        try {
            var from = 0
            val size = 100

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .query { q ->
                            q.bool { b ->
                                b.should { sh ->
                                    // Exact match
                                    sh.term { t ->
                                        t.field("metadata.path")
                                            .value(FieldValue.of(documentPath))
                                    }
                                }
                            }
                        }
                        .source { src ->
                            src.filter { f ->
                                f.includes(
                                    listOf(
                                        "id",
                                        "metadata.id",
                                        "metadata.title",
                                        "metadata.path"
                                    )
                                )
                            }
                        }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                hits.forEach { hit ->
                    val source = hit.source()
                    if (source != null) {
                        // Get Confluence ID from metadata.id field
                        val confluenceId = source["metadata.id"]?.toString()

                        if (confluenceId != null) {
                            val baseId = confluenceId.extractChunk()

                            if (!documents.containsKey(baseId)) {
                                val title = source["metadata.title"]?.toString() ?: "Untitled"
                                val path = source["metadata.path"]?.toString() ?: ""

                                documents[baseId] = Document(
                                    id = baseId,
                                    title = title,
                                    path = path
                                )
                            }
                        }
                    }
                }

                if (hits.size < size) break
                from += size
            }

            log.info { "Found ${documents.size} documents under path: $documentPath" }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch documents by path: $documentPath, error=${e.message}" }
        }

        documents.values.toList().sortedBy { it.title }
    }

    /**
     * Get all unique paths from OpenSearch
     */
    fun searchAllPaths(): List<String> {
        val paths = mutableSetOf<String>()

        try {
            var from = 0
            val size = 200

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .source { src -> src.filter { f -> f.includes(listOf("metadata.path")) } }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                // Extract paths
                hits.forEach { hit ->
                    val source = hit.source()
                    val path = source?.get("metadata.path")?.toString()
                    path?.let {
                        if (it.isNotBlank()) paths.add(it)
                    }
                }

                // Check if there are more pages
                if (hits.size < size) break
                from += size
            }

            log.info { "Found ${paths.size} unique paths from index" }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch paths from index: ${e.message}" }
        }

        return paths.toList().sorted()
    }

    private fun buildHierarchyRecursive(
        documents: Map<String, List<Pair<String, String>>>,
        parentPath: String = ""
    ): List<DocumentNode> {
        val folders = mutableMapOf<String, MutableList<String>>() // folderName -> list of full paths

        // Group paths by immediate child folder
        documents.keys.forEach { path ->
            if (parentPath.isEmpty()) {
                // Root level
                val parts = path.split(" > ")
                if (parts.isNotEmpty()) {
                    val folderName = parts[0]
                    folders.getOrPut(folderName) { mutableListOf() }.add(path)
                }
            } else {
                // Check if this path is under parent
                if (path.startsWith("$parentPath > ")) {
                    val remaining = path.substring(parentPath.length + 3) // +3 for " > "
                    val parts = remaining.split(" > ")
                    if (parts.isNotEmpty()) {
                        val folderName = parts[0]
                        folders.getOrPut(folderName) { mutableListOf() }.add(path)
                    }
                }
            }
        }

        // Build folder nodes
        return folders.map { (folderName, paths) ->
            val currentPath = if (parentPath.isEmpty()) folderName else "$parentPath > $folderName"

            // Get documents directly in this folder
            val docsInFolder = documents[currentPath]?.map { (id, title) ->
                Document(
                    id = id,
                    title = title,
                    path = currentPath
                )
            }?.distinctBy { it.id } ?: emptyList()

            // Get child folders
            val childPaths = paths.filter { it != currentPath }
            val childDocuments = childPaths.associateWith { documents[it] ?: emptyList() }
            val children = if (childDocuments.isNotEmpty()) {
                buildHierarchyRecursive(documents, currentPath)
            } else {
                emptyList()
            }

            DocumentNode(
                name = folderName,
                path = currentPath,
                children = children,
                documents = docsInFolder
            )
        }.sortedBy { it.name }
    }
}
