package com.okestro.okchat.task

import com.okestro.okchat.confluence.service.ConfluenceService
import com.okestro.okchat.confluence.service.ContentHierarchy
import com.okestro.okchat.confluence.service.ContentNode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.task.configuration.EnableTask
import org.springframework.stereotype.Component
import org.typesense.api.Client
import org.typesense.model.SearchParameters

/**
 * Spring Cloud Task for syncing Confluence content to Typesense vector store
 *
 * This task can be run as:
 * 1. Standalone application
 * 2. Kubernetes Job/CronJob
 * 3. Scheduled task in cloud environments
 *
 * Run with: --spring.cloud.task.name=confluence-sync-task
 */
@Component
@EnableTask
@ConditionalOnProperty(
    name = ["spring.cloud.task.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class ConfluenceSyncTask(
    private val confluenceService: ConfluenceService,
    private val vectorStore: VectorStore,
    private val typesenseClient: Client,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) {
        log.info { "========== Start Confluence Sync Task ==========" }

        try {
            // Parse command line arguments
            val spaceKey = parseSpaceKey(args) ?: run {
                log.error { "spaceKey parameter is required. Usage: spaceKey=XXXX" }
                throw IllegalArgumentException("spaceKey parameter is required")
            }

            log.info { "Target space: $spaceKey" }

            // 1. Fetch Confluence content hierarchy
            log.info { "1. Fetching Confluence content..." }
            val spaceId = confluenceService.getSpaceIdByKey(spaceKey)
            val hierarchy = confluenceService.getSpaceContentHierarchy(spaceId)

            log.info { "✓ Retrieved ${hierarchy.getTotalCount()} contents" }
            log.info { "  - Folders: ${hierarchy.getAllFolders().size}" }
            log.info { "  - Pages: ${hierarchy.getAllPages().size}" }

            // 2. Get existing document IDs for this space
            log.info { "2. Fetching existing documents for space: $spaceKey..." }
            val existingIds = try {
                getExistingDocumentIds(spaceKey)
            } catch (e: Exception) {
                log.warn { "Failed to fetch existing documents (might be first sync): ${e.message}" }
                emptySet()
            }
            log.info { "✓ Found ${existingIds.size} existing documents" }

            // 3. Convert to vector store documents
            log.info { "3. Converting to vector store documents..." }
            val documents = convertToDocuments(hierarchy, spaceKey)
            val currentIds = documents.map { it.id }.toSet()
            log.info { "✓ Converted ${documents.size} documents" }

            // 4. Delete removed documents
            val deletedIds = existingIds - currentIds
            if (deletedIds.isNotEmpty()) {
                log.info { "4. Deleting ${deletedIds.size} removed documents..." }
                try {
                    deletedIds.forEach { id ->
                        vectorStore.delete(listOf(id))
                    }
                    log.info { "✓ Deleted removed documents" }
                } catch (e: Exception) {
                    log.warn { "Failed to delete some documents: ${e.message}" }
                }
            } else {
                log.info { "4. No documents to delete" }
            }

            // 5. Store/Update in Typesense
            log.info { "5. Storing/Updating in Typesense..." }
            vectorStore.add(documents)
            log.info { "✓ Stored/Updated ${documents.size} documents (${currentIds.size - existingIds.size} new, ${existingIds.intersect(currentIds).size} updated)" }

            // 5. Summary
            log.info { "========== Sync Completed ==========" }
            log.info { "Space: $spaceKey" }
            log.info { "Processed pages: ${documents.size}" }
        } catch (e: Exception) {
            log.error(e) { "Error occurred during Confluence sync: ${e.message}" }
            throw e // Rethrow to mark task as failed
        }
    }

    /**
     * Convert Confluence hierarchy to vector store documents
     */
    private fun convertToDocuments(hierarchy: ContentHierarchy, spaceKey: String): List<Document> {
        val documents = mutableListOf<Document>()

        // Only convert pages (not folders)
        hierarchy.getAllPages().forEach { node ->
            // Create document with page title and metadata
            val document = Document(
                node.id, // Use Confluence page ID as document ID
                buildPageContent(node, hierarchy),
                mapOf(
                    "id" to node.id,
                    "title" to node.title,
                    "type" to "confluence-page",
                    "spaceKey" to spaceKey,
                    "path" to getPagePath(node, hierarchy)
                )
            )
            documents.add(document)
        }

        return documents
    }

    /**
     * Build page content for embedding
     */
    private fun buildPageContent(node: ContentNode, hierarchy: ContentHierarchy): String {
        val path = getPagePath(node, hierarchy)
        return """
            Title: ${node.title}
            Path: $path
            Page ID: ${node.id}

            This page is managed in Confluence.
        """.trimIndent()
    }

    /**
     * Get full path to a page (breadcrumb)
     */
    private fun getPagePath(node: ContentNode, hierarchy: ContentHierarchy): String {
        val pathNodes = hierarchy.getPathToNode(node.id) ?: return node.title
        return pathNodes.joinToString(" > ") { it.title }
    }

    /**
     * Get existing document IDs for a specific space from vector store
     * Uses Typesense client directly to avoid embedding API calls
     */
    private fun getExistingDocumentIds(spaceKey: String): Set<String> {
        val searchParameters = SearchParameters()
            .q("*") // Match all documents
            .queryBy("id") // Query by id field
            .filterBy("spaceKey:=$spaceKey") // Filter by spaceKey
            .perPage(250) // Max per page

        val documentIds = mutableSetOf<String>()
        var page = 1

        // Paginate through all results
        while (true) {
            searchParameters.page(page)
            val searchResult = typesenseClient.collections(collectionName).documents().search(searchParameters)

            val hits = searchResult.hits ?: break
            if (hits.isEmpty()) break

            // Extract document IDs
            hits.forEach { hit ->
                val doc = hit.document
                if (doc != null && doc.containsKey("id")) {
                    documentIds.add(doc["id"].toString())
                }
            }

            // Check if there are more pages
            if (hits.size < 250) break
            page++
        }

        return documentIds
    }

    /**
     * Parse spaceKey from command line arguments
     * Expected format: spaceKey=XXXX
     */
    private fun parseSpaceKey(args: Array<out String?>): String? {
        return args.filterNotNull()
            .firstOrNull { it.startsWith("spaceKey=") }
            ?.substringAfter("spaceKey=")
            ?.takeIf { it.isNotBlank() }
    }
}
