package com.okestro.okchat.task

import com.okestro.okchat.ai.service.chunking.ChunkingStrategy
import com.okestro.okchat.ai.service.extraction.DocumentKeywordExtractionService
import com.okestro.okchat.confluence.config.ConfluenceProperties
import com.okestro.okchat.confluence.model.ContentHierarchy
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.service.ConfluenceService
import com.okestro.okchat.confluence.service.PdfAttachmentService
import com.okestro.okchat.confluence.util.ContentHierarchyVisualizer
import com.okestro.okchat.search.support.MetadataFields
import com.okestro.okchat.search.support.metadata
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Spring Cloud Task for syncing Confluence content to OpenSearch vector store
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
    private val pdfAttachmentService: PdfAttachmentService,
    private val vectorStore: VectorStore,
    private val openSearchClient: OpenSearchClient,
    private val documentKeywordExtractionService: DocumentKeywordExtractionService,
    private val chunkingStrategy: ChunkingStrategy,
    private val confluenceProperties: ConfluenceProperties,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String,
    private val meterRegistry: MeterRegistry
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) = runBlocking {
        log.info { "[ConfluenceSync] Starting Confluence sync task" }
        val sample = Timer.start(meterRegistry)
        val tags = Tags.of("task", "confluence-sync")

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

            log.info { "[ConfluenceSync] Retrieved contents: total=${hierarchy.getTotalCount()}, folders=${hierarchy.getAllFolders().size}, pages=${hierarchy.getAllPages().size}" }

            // 2. Get existing document IDs for this space
            log.info { "2. Fetching existing documents for space: $spaceKey..." }
            val existingIds = try {
                getExistingDocumentIds(spaceKey)
            } catch (e: Exception) {
                log.warn { "Failed to fetch existing documents (might be first sync): ${e.message}" }
                emptySet()
            }
            log.info { "[ConfluenceSync] Found existing documents: count=${existingIds.size}" }

            // 3. Convert to vector store documents (parallel processing)
            log.info { "3. Converting to vector store documents (parallel processing)..." }
            val documents = convertToDocuments(hierarchy, spaceKey)
            val currentIds = documents.map { it.id }.toSet()
            log.info { "[ConfluenceSync] Converted documents: count=${documents.size}" }

            // 4. Delete removed documents
            val deletedIds = existingIds - currentIds
            if (deletedIds.isNotEmpty()) {
                log.info { "4. Deleting ${deletedIds.size} removed documents..." }
                try {
                    vectorStore.delete(deletedIds.toList())
                    log.info { "[ConfluenceSync] Deleted removed documents: count=${deletedIds.size}" }
                } catch (e: Exception) {
                    log.warn { "Failed to delete some documents: ${e.message}" }
                }
            } else {
                log.info { "4. No documents to delete" }
            }

            // 5. Store/Update in OpenSearch (batch processing to avoid timeout)
            log.info { "5. Storing/Updating in OpenSearch (batch processing)..." }

            // Get initial document count
            val initialDocCount = getOpenSearchDocumentCount()
            log.info { "  Initial OpenSearch document count: $initialDocCount" }

            val batchSize = 10 // Process 10 documents at a time
            val batches = documents.chunked(batchSize)
            var successCount = 0
            val failedBatches = mutableListOf<Int>()

            batches.forEachIndexed { batchIndex, batch ->
                val batchNum = batchIndex + 1
                try {
                    log.info { "  [Batch $batchNum/${batches.size}] Adding ${batch.size} documents..." }

                    // Get count before add
                    val countBefore = getOpenSearchDocumentCount()

                    // Add documents via VectorStore (now handles errors gracefully)
                    vectorStore.add(batch)

                    // Verify count increased
                    val countAfter = getOpenSearchDocumentCount()
                    val actualAdded = countAfter - countBefore

                    if (actualAdded.toInt() == batch.size) {
                        successCount += batch.size
                        log.info { "  [ConfluenceSync][Batch $batchNum/${batches.size}] Successfully added documents: batch_size=${batch.size}, count_before=$countBefore, count_after=$countAfter" }
                    } else {
                        log.warn { "  [ConfluenceSync][Batch $batchNum/${batches.size}] Document count mismatch: expected=${batch.size}, actual=$actualAdded, count_before=$countBefore, count_after=$countAfter" }
                        successCount += actualAdded.toInt()
                    }

                    log.info { "  [Batch $batchNum/${batches.size}] Progress: $successCount/${documents.size}" }
                } catch (e: Exception) {
                    log.error(e) { "  [Batch $batchNum/${batches.size}] ✗ Failed to add batch: ${e.message}" }
                    log.error { "  [Batch $batchNum/${batches.size}] Batch IDs: ${batch.map { it.id }.take(5)}..." }
                    log.error { "  [Batch $batchNum/${batches.size}] Stack trace: ${e.stackTraceToString()}" }
                    failedBatches.add(batchNum)
                    // Continue processing remaining batches instead of failing entire task
                    log.warn { "  [Batch $batchNum/${batches.size}] Continuing with next batch..." }
                }
            }

            // Log failed batches summary
            if (failedBatches.isNotEmpty()) {
                log.error { "  [ConfluenceSync] Failed batches: ${failedBatches.joinToString(", ")} (total: ${failedBatches.size}/${batches.size})" }
            }

            // Verify final count
            val finalDocCount = getOpenSearchDocumentCount()
            log.info { "  Final OpenSearch document count: $finalDocCount (increased by ${finalDocCount - initialDocCount})" }

            if ((finalDocCount - initialDocCount).toInt() != successCount) {
                log.error { "  [ConfluenceSync] Document count mismatch: expected_added=$successCount, actual_increase=${finalDocCount - initialDocCount}" }
            }

            log.info { "[ConfluenceSync] Stored/updated documents: total=$successCount, new=${currentIds.size - existingIds.size}, updated=${existingIds.intersect(currentIds).size}" }

            // 6. Summary
            log.info { "[ConfluenceSync] Sync completed: space_key=$spaceKey, processed_pages=${documents.size}" }

            // Record metrics
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "success")))
            meterRegistry.counter("task.execution.count", tags.and("status", "success")).increment()
            meterRegistry.counter("task.confluence.sync.documents.processed", tags).increment(documents.size.toDouble())
            meterRegistry.counter("task.confluence.sync.documents.added", tags).increment((currentIds.size - existingIds.size).toDouble())
            meterRegistry.counter("task.confluence.sync.documents.updated", tags).increment((existingIds.intersect(currentIds).size).toDouble())
            meterRegistry.counter("task.confluence.sync.documents.deleted", tags).increment((existingIds.size - currentIds.size + (currentIds.size - existingIds.intersect(currentIds).size)).toDouble().coerceAtLeast(0.0)) // Approximated for deleted
        } catch (e: Exception) {
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "failure")))
            meterRegistry.counter("task.execution.count", tags.and("status", "failure")).increment()
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

                    // 빈 제목/내용도 저장 (모두 의미가 있을 수 있음)
                    val pageTitle = node.title.ifBlank { "Untitled-${node.id}" }
                    val pageContent = node.body?.let { stripHtml(it) } ?: ""

                    if (pageContent.isBlank()) {
                        log.info { "[ConfluenceSync][$pageNum/$totalPages] Processing page with empty content: id=${node.id}, title='$pageTitle'" }
                    }

                    // Only log every 10th page to reduce noise
                    if (pageNum % 10 == 0 || pageNum == 1 || pageNum == totalPages) {
                        log.info { "[ConfluenceSync][$pageNum/$totalPages] Processing: title='$pageTitle', content_length=${pageContent.length}" }
                    }

                    // Extract keywords FIRST (before building content)
                    // Use semaphore to limit concurrent API calls
                    // Skip keyword extraction for empty content to save API calls
                    val keywords = if (pageContent.isBlank()) {
                        emptyList()
                    } else {
                        apiCallSemaphore.withPermit {
                            try {
                                val documentMessage = extractFromDocument(pageContent, pageTitle)
                                documentKeywordExtractionService.execute(documentMessage)
                            } catch (_: Exception) {
                                log.warn { "[ConfluenceSync] Keyword extraction failed: page_title='$pageTitle'" }
                                emptyList()
                            }
                        }
                    }

                    // Extract path keywords (each level of hierarchy becomes a keyword)
                    val pathKeywords = path.split(" > ").map { it.trim() }.filter { it.isNotBlank() }

                    // Combine content keywords + path keywords for better hierarchical search
                    val allKeywords = (keywords + pathKeywords).distinct()

                    // Reduced logging for keywords

                    // Create initial document with metadata including ALL keywords (content + path)
                    // For empty content, store at least the title and metadata
                    val documentContent = pageContent.ifBlank { pageTitle }
                    val currentSpaceKey = spaceKey
                    val currentPath = path

                    val wikiBaseUrl = confluenceProperties.baseUrl.removeSuffix("/api/v2").removeSuffix("/")
                    val pageUrl = "$wikiBaseUrl/wiki/spaces/$currentSpaceKey/pages/${node.id}"

                    val baseMetadata = metadata {
                        this.id = node.id
                        this.title = pageTitle
                        this.type = "confluence-page"
                        this.spaceKey = currentSpaceKey
                        this.path = currentPath
                        this.keywords = allKeywords

                        // Additional page properties
                        property(MetadataFields.Additional.IS_EMPTY, pageContent.isBlank())
                        property(MetadataFields.Additional.WEB_URL, pageUrl)
                    }

                    val baseDocument = Document(
                        node.id,
                        documentContent,
                        baseMetadata.toMap()
                    )

                    // Split document into chunks if too large
                    val chunks = try {
                        chunkingStrategy.chunk(baseDocument)
                    } catch (e: Exception) {
                        log.warn { "[ConfluenceSync] Failed to chunk document: page_title='$pageTitle', error=${e.message}" }
                        // If chunking fails and content is too large, truncate it
                        val maxLength = 8000 // Safe limit for embedding models
                        if (documentContent.length > maxLength) {
                            log.warn { "[ConfluenceSync] Content too large (${documentContent.length} chars), truncating to $maxLength chars" }
                            val truncated = Document(
                                baseDocument.id,
                                documentContent.take(maxLength) + "... [truncated]",
                                baseDocument.metadata + mapOf("truncated" to true)
                            )
                            listOf(truncated)
                        } else {
                            listOf(baseDocument)
                        }
                    }

                    // If single chunk, use original page ID
                    // If multiple chunks, append chunk index to ID
                    val resultDocuments = if (chunks.size == 1) {
                        chunks
                    } else {
                        if (pageNum % 10 == 0 || chunks.size > 5) {
                            log.info { "[ConfluenceSync] Split into ${chunks.size} chunks: page_id=${node.id}" }
                        }
                        chunks.mapIndexed { chunkIndex, chunk ->
                            val chunkMetadata = metadata {
                                this.id = node.id
                                this.keywords = allKeywords

                                // Chunk information
                                property(MetadataFields.Additional.CHUNK_INDEX, chunkIndex)
                                property(MetadataFields.Additional.TOTAL_CHUNKS, chunks.size)
                            }

                            Document(
                                "${node.id}_chunk_$chunkIndex",
                                chunk.text ?: "",
                                chunk.metadata + chunkMetadata.toMap()
                            )
                        }
                    }

                    // Process PDF attachments for this page
                    val pdfDocuments = try {
                        pdfAttachmentService.processPdfAttachments(
                            pageId = node.id,
                            pageTitle = pageTitle,
                            spaceKey = spaceKey,
                            path = path
                        )
                    } catch (e: Exception) {
                        log.warn { "[ConfluenceSync] Failed to process PDF attachments: page_title='$pageTitle', error=${e.message}" }
                        emptyList()
                    }

                    if (pdfDocuments.isNotEmpty() && (pageNum % 10 == 0 || pageNum == 1 || pageNum == totalPages)) {
                        log.info { "[ConfluenceSync][$pageNum/$totalPages] Found ${pdfDocuments.size} PDF document(s) for page: $pageTitle" }
                    }

                    // Progress logged only every 10 pages above

                    resultDocuments + pdfDocuments
                }
            }.awaitAll().flatten()
        }

        documents.addAll(allDocuments)

        return documents
    }

    /**
     * Extract keywords from document content for search indexing.
     * Handles both content and title to generate comprehensive keywords.
     */
    suspend fun extractFromDocument(content: String, title: String? = null): String {
        return """
            Title: ${title ?: "N/A"}
            Content: ${content.take(2000)}...
        """.trimIndent()
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
     * Get existing document IDs for a specific space from OpenSearch
     */
    private fun getExistingDocumentIds(spaceKey: String): Set<String> {
        val documentIds = mutableSetOf<String>()

        try {
            // Search with filter for spaceKey, paginate through all results
            var from = 0
            val size = 250

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .query { q ->
                            q.term { t ->
                                // Use flat field name (metadata is flattened with dot notation)
                                t.field(MetadataFields.SPACE_KEY)
                                    .value(org.opensearch.client.opensearch._types.FieldValue.of(spaceKey))
                            }
                        }
                        .source { src -> src.filter { f -> f.includes(listOf("id", MetadataFields.ID)) } }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                // Extract document IDs (청크 ID가 아닌 실제 문서 ID를 가져와야 함)
                hits.forEach { hit ->
                    val source = hit.source()
                    // 우선순위: metadata.id (실제 Confluence 페이지 ID) > id (청크 ID) > _id
                    val id = when {
                        source?.containsKey(MetadataFields.ID) == true -> source[MetadataFields.ID]?.toString()
                        source?.containsKey("id") == true -> {
                            // id 필드에서 청크 suffix 제거 (예: "123_chunk_0" -> "123")
                            val rawId = source["id"]?.toString()
                            rawId?.let {
                                if (it.contains("_chunk_")) it.substringBefore("_chunk_") else it
                            }
                        }
                        else -> hit.id()
                    }
                    id?.let { documentIds.add(it) }
                }

                // Check if there are more pages
                if (hits.size < size) break
                from += size
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch existing document IDs: ${e.message}" }
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
     * Get current document count from OpenSearch directly
     */
    private fun getOpenSearchDocumentCount(): Long {
        return try {
            val countResponse = openSearchClient.count { c ->
                c.index(indexName)
            }
            countResponse.count()
        } catch (e: Exception) {
            log.warn { "Failed to get OpenSearch document count: ${e.message}" }
            -1 // Return -1 to indicate failure
        }
    }
}
