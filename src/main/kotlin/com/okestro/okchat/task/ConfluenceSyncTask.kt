package com.okestro.okchat.task

import com.okestro.okchat.ai.support.KeywordExtractionService
import com.okestro.okchat.chunking.ChunkingStrategy
import com.okestro.okchat.confluence.model.ContentHierarchy
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.service.ConfluenceService
import com.okestro.okchat.confluence.util.ContentHierarchyVisualizer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.typesense.api.Client
import org.typesense.model.SearchParameters
import kotlin.collections.emptyList

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
@ConditionalOnProperty(
    name = ["task.confluence-sync.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class ConfluenceSyncTask(
    private val confluenceService: ConfluenceService,
    private val vectorStore: VectorStore,
    private val typesenseClient: Client,
    private val keywordExtractionService: KeywordExtractionService,
    private val chunkingStrategy: ChunkingStrategy,
    private val embeddingModel: org.springframework.ai.embedding.EmbeddingModel,
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
            val hierarchy = confluenceService.getSpaceContentHierarchy(spaceId).apply {
                log.info { "Content Hierarchy:\n${ContentHierarchyVisualizer.visualize(this)}" }
            }

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

            // 3. Convert to vector store documents (parallel processing)
            log.info { "3. Converting to vector store documents (parallel processing)..." }
            val documents = runBlocking {
                convertToDocuments(hierarchy, spaceKey)
            }
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

            // 5. Store/Update in Typesense (batch processing to avoid timeout)
            log.info { "5. Storing/Updating in Typesense (batch processing)..." }

            // Get initial document count
            val initialDocCount = getTypesenseDocumentCount()
            log.info { "  Initial Typesense document count: $initialDocCount" }

            val batchSize = 10 // Process 10 documents at a time
            val batches = documents.chunked(batchSize)
            var successCount = 0

            batches.forEachIndexed { batchIndex, batch ->
                val batchNum = batchIndex + 1
                try {
                    log.info { "  [Batch $batchNum/${batches.size}] Adding ${batch.size} documents..." }

                    // Get count before add
                    val countBefore = getTypesenseDocumentCount()

                    // Add documents directly to Typesense (bypassing Spring AI VectorStore)
                    // Spring AI's batching strategy was not persisting documents properly
                    log.debug { "  [Batch $batchNum/${batches.size}] Persisting ${batch.size} documents directly to Typesense..." }

                    batch.forEach { document ->
                        try {
                            val typesenseDoc = convertToTypesenseDocument(document)
                            // upsert() immediately sends HTTP POST to Typesense (synchronous, no flush needed)
                            typesenseClient.collections(collectionName).documents().upsert(typesenseDoc)
                        } catch (e: Exception) {
                            log.error(e) { "  Failed to upsert document ${document.id}: ${e.message}" }
                            log.error { "  Document content length: ${document.text?.length ?: 0}" }
                            throw e
                        }
                    }

                    // Verify count increased
                    val countAfter = getTypesenseDocumentCount()
                    val actualAdded = countAfter - countBefore

                    if (actualAdded.toInt() == batch.size) {
                        successCount += batch.size
                        log.info { "  [Batch $batchNum/${batches.size}] ✓ Successfully added ${batch.size} documents (Verified: $countBefore → $countAfter)" }
                    } else {
                        log.warn { "  [Batch $batchNum/${batches.size}] ⚠ Added ${batch.size} but only $actualAdded appeared in Typesense ($countBefore → $countAfter)" }
                        successCount += actualAdded.toInt()
                    }

                    log.info { "  [Batch $batchNum/${batches.size}] Progress: $successCount/${documents.size}" }
                } catch (e: Exception) {
                    log.error(e) { "  [Batch $batchNum/${batches.size}] ✗ Failed to add batch: ${e.message}" }
                    log.error { "  [Batch $batchNum/${batches.size}] Batch IDs: ${batch.map { it.id }.take(5)}..." }
                    log.error { "  [Batch $batchNum/${batches.size}] Stack trace: ${e.stackTraceToString()}" }
                    throw e // Rethrow to mark task as failed
                }
            }

            // Verify final count
            val finalDocCount = getTypesenseDocumentCount()
            log.info { "  Final Typesense document count: $finalDocCount (increased by ${finalDocCount - initialDocCount})" }

            if ((finalDocCount - initialDocCount).toInt() != successCount) {
                log.error { "  ⚠️ WARNING: Expected to add $successCount documents but Typesense count only increased by ${finalDocCount - initialDocCount}" }
            }

            log.info { "✓ Stored/Updated $successCount documents (${currentIds.size - existingIds.size} new, ${existingIds.intersect(currentIds).size} updated)" }

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
     * Splits large pages into chunks to fit embedding model token limits
     * Extracts keywords for each document for better searchability
     */
    private suspend fun convertToDocuments(hierarchy: ContentHierarchy, spaceKey: String): List<Document> {
        val documents = mutableListOf<Document>()
        val allPages = hierarchy.getAllPages()
        val totalPages = allPages.size

        log.info { "Converting $totalPages pages to vector store documents..." }

        // Limit concurrent API calls to prevent timeout/rate limit issues
        // Max 5 concurrent OpenAI API calls at a time
        val apiCallSemaphore = Semaphore(5)

        // Only convert pages (not folders) - Process in parallel with concurrency limit
        val allDocuments = coroutineScope {
            allPages.mapIndexed { index, node ->
                async(Dispatchers.IO) {
                    val pageNum = index + 1
                    val path = getPagePath(node, hierarchy)

                    log.info { "[$pageNum/$totalPages] Processing: '${node.title}' (ID: ${node.id})" }
                    log.info { "  └─ Path: $path" }

                    // Extract keywords FIRST (before building content)
                    // Use semaphore to limit concurrent API calls
                    log.info { "  └─ Extracting keywords..." }
                    val keywords = apiCallSemaphore.withPermit {
                        try {
                            // Build preliminary content for keyword extraction
                            val preliminaryContent = node.body?.let { stripHtml(it) } ?: ""
                            keywordExtractionService.extractKeywordsFromContentAndTitle(preliminaryContent, node.title)
                        } catch (e: Exception) {
                            log.warn { "  └─ Failed to extract keywords for '${node.title}': ${e.message}" }
                            emptyList()
                        }
                    }

                    // Extract path keywords (each level of hierarchy becomes a keyword)
                    // Example: "PPP 개발 Repositories > Backend > API" → ["PPP 개발 Repositories", "Backend", "API"]
                    val pathKeywords = path.split(" > ").map { it.trim() }.filter { it.isNotBlank() }

                    // Combine content keywords + path keywords for better hierarchical search
                    val allKeywords = (keywords + pathKeywords).distinct()

                    if (keywords.isNotEmpty()) {
                        log.info { "  └─ Content keywords: ${keywords.joinToString(", ")}" }
                    } else {
                        log.info { "  └─ No content keywords extracted" }
                    }
                    log.info { "  └─ Path keywords: ${pathKeywords.joinToString(", ")}" }
                    log.info { "  └─ Total keywords: ${allKeywords.size} (${keywords.size} content + ${pathKeywords.size} path)" }

                    // Build page content WITH keywords included for search
                    val pageContent = buildPageContent(node, hierarchy, allKeywords)
                    val contentLength = pageContent.length

                    // Create initial document with metadata including ALL keywords (content + path)
                    val baseDocument = Document(
                        node.id,
                        pageContent,
                        mapOf(
                            "id" to node.id,
                            "title" to node.title,
                            "type" to "confluence-page",
                            "spaceKey" to spaceKey,
                            "path" to path,
                            "keywords" to allKeywords.joinToString(", ")
                        )
                    )

                    // Split document into chunks if too large
                    log.info { "  └─ Content length: $contentLength chars, splitting if needed..." }
                    val chunks = try {
                        chunkingStrategy.chunk(baseDocument)
                    } catch (e: Exception) {
                        log.warn { "  └─ Failed to chunk '${node.title}': ${e.message}. Using original document." }
                        listOf(baseDocument)
                    }

                    // If single chunk, use original page ID
                    // If multiple chunks, append chunk index to ID
                    val resultDocuments = if (chunks.size == 1) {
                        log.info { "  └─ ✓ Document created (single chunk)" }
                        chunks
                    } else {
                        log.info { "  └─ ✓ Document split into ${chunks.size} chunks" }
                        chunks.mapIndexed { chunkIndex, chunk ->
                            Document(
                                "${node.id}_chunk_$chunkIndex",
                                chunk.text ?: "",
                                chunk.metadata + mapOf(
                                    "id" to node.id,
                                    "chunkIndex" to chunkIndex,
                                    "totalChunks" to chunks.size,
                                    "keywords" to allKeywords.joinToString(", ")
                                )
                            )
                        }
                    }

                    log.info { "  └─ Progress: $pageNum/$totalPages (${String.format("%.1f", (pageNum.toDouble() / totalPages) * 100)}%)" }
                    log.info { "" } // Empty line for readability

                    resultDocuments
                }
            }.awaitAll().flatten()
        }

        documents.addAll(allDocuments)

        return documents
    }

    /**
     * Build page content for embedding and search
     * Includes extracted keywords for better searchability
     */
    private fun buildPageContent(
        node: ContentNode,
        hierarchy: ContentHierarchy,
        keywords: List<String> = emptyList()
    ): String {
        val path = getPagePath(node, hierarchy)
        val cleanContent = node.body?.let { stripHtml(it) } ?: ""

        return buildString {
            append("Title: ${node.title}\n")
            append("Path: $path\n")
            append("Page ID: ${node.id}\n")

            // Include keywords for better search matching
            if (keywords.isNotEmpty()) {
                append("Keywords: ${keywords.joinToString(", ")}\n")
            }

            append("\n")
            if (cleanContent.isNotBlank()) {
                append("Content:\n")
                append(cleanContent)
            } else {
                append("This page has no content.")
            }
        }
    }

    /**
     * Strip HTML tags from content
     */
    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<.*?>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
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

    /**
     * Get current document count from Typesense directly
     * This bypasses Spring AI caching to get real-time count
     */
    private fun getTypesenseDocumentCount(): Long {
        return try {
            val collection = typesenseClient.collections(collectionName).retrieve()
            collection.numDocuments ?: 0
        } catch (e: Exception) {
            log.warn { "Failed to get Typesense document count: ${e.message}" }
            -1 // Return -1 to indicate failure
        }
    }

    /**
     * Convert Spring AI Document to Typesense document format
     * * IMPORTANT: Typesense Java Client's upsert() is synchronous and sends HTTP POST immediately.
     * No flush/commit is needed - the document is persisted as soon as upsert() returns successfully.
     * * Note: Spring AI Document doesn't store embedding in the Document object itself.
     * The embedding is generated and added by VectorStore during the add() operation.
     * We need to generate it here manually using the embeddingModel.
     */
    private fun convertToTypesenseDocument(document: org.springframework.ai.document.Document): Map<String, Any> {
        val metadata = document.metadata

        // Generate embedding for this document
        val embedding: List<Float> = try {
            // embed() returns float[] which we convert to List<Float>
            embeddingModel.embed(document.text ?: "").toList()
        } catch (e: Exception) {
            log.error(e) { "Failed to generate embedding for document ${document.id}: ${e.message}" }
            throw IllegalStateException("Failed to generate embedding for document ${document.id}", e)
        }

        // Validate embedding dimension
        if (embedding.isEmpty()) {
            throw IllegalStateException("Document ${document.id} generated empty embedding")
        }
        if (embedding.size != 1536) {
            throw IllegalStateException("Document ${document.id} has invalid embedding dimension: ${embedding.size} (expected 1536)")
        }

        log.trace { "Converting document ${document.id}: embedding size=${embedding.size}, content length=${document.text?.length ?: 0}" }

        return mapOf(
            "id" to document.id,
            "content" to (document.text ?: ""),
            "embedding" to embedding,
            "metadata.title" to (metadata["title"] ?: ""),
            "metadata.keywords" to (metadata["keywords"] ?: ""),
            "metadata.type" to (metadata["type"] ?: ""),
            "metadata.spaceKey" to (metadata["spaceKey"] ?: ""),
            "metadata.path" to (metadata["path"] ?: "")
        )
    }
}
