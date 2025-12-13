package com.okestro.okchat.task

import com.github.f4b6a3.tsid.TsidCreator
import com.okestro.okchat.ai.service.chunking.ChunkingStrategy
import com.okestro.okchat.ai.service.extraction.DocumentKeywordExtractionService
import com.okestro.okchat.confluence.config.ConfluenceProperties
import com.okestro.okchat.confluence.model.ContentHierarchy
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.service.ConfluenceService
import com.okestro.okchat.confluence.service.PdfAttachmentService
import com.okestro.okchat.confluence.util.ContentHierarchyVisualizer
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.model.value.ContentPath
import com.okestro.okchat.knowledge.repository.DocumentRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import com.okestro.okchat.search.support.MetadataFields
import com.okestro.okchat.search.support.metadata
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Spring Cloud Task for syncing Confluence content to OpenSearch vector store and RDB
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
    private val documentKeywordExtractionService: DocumentKeywordExtractionService,
    private val chunkingStrategy: ChunkingStrategy,
    private val confluenceProperties: ConfluenceProperties,
    private val meterRegistry: MeterRegistry,
    private val observationRegistry: ObservationRegistry,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val documentRepository: DocumentRepository
) : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    override fun run(vararg args: String?) {
        val observation = Observation.createNotStarted("task.confluence-sync", observationRegistry)
        observation.observe {
            runBlocking(MDCContext()) {
                executeTask()
            }
        }
    }

    private suspend fun executeTask() {
        log.info { "[ConfluenceSync] Starting Confluence sync task" }
        // Find enabled Confluence KBs
        val knowledgeBases: List<KnowledgeBase> = withContext(Dispatchers.IO + MDCContext()) {
            knowledgeBaseRepository.findAllByEnabledTrueAndType(KnowledgeBaseType.CONFLUENCE)
        }

        if (knowledgeBases.isEmpty()) {
            log.warn { "No enabled Confluence Knowledge Bases found." }
            return
        }

        log.info { "Found ${knowledgeBases.size} knowledge bases to sync" }

        knowledgeBases.forEach { kb ->
            syncKnowledgeBase(kb)
        }
    }

    private suspend fun syncKnowledgeBase(kb: KnowledgeBase) {
        val spaceKey = kb.config["spaceKey"] as? String ?: run {
            log.error { "Knowledge Base ${kb.name} (ID: ${kb.id}) has no spaceKey in config" }
            return
        }

        log.info { "Starting sync for KB: ${kb.name} (Space: $spaceKey)" }
        val sample = Timer.start(meterRegistry)
        val tags = Tags.of("task", "confluence-sync", "kb", kb.name)

        try {
            // 1. Fetch Confluence content hierarchy
            log.info { "1. Fetching Confluence content for space $spaceKey..." }
            val spaceId = confluenceService.getSpaceIdByKey(spaceKey)

            val hierarchy = confluenceService.getSpaceContentHierarchy(spaceId).apply {
                log.info { "Content Hierarchy:\n${ContentHierarchyVisualizer.visualize(this)}" }
            }

            log.info { "[ConfluenceSync] Retrieved contents: total=${hierarchy.getTotalCount()}, folders=${hierarchy.getAllFolders().size}, pages=${hierarchy.getAllPages().size}" }

            // 2. Get existing document IDs for this space (from RDB for reliability)
            log.info { "2. Fetching existing documents for KB ${kb.name}..." }
            val existingDocs: List<com.okestro.okchat.knowledge.model.entity.Document> = withContext(Dispatchers.IO + MDCContext()) {
                documentRepository.findAllByKnowledgeBaseId(kb.id!!)
            }
            val existingExternalIds = existingDocs.map { it.externalId }.toSet()
            val existingDocMap = existingDocs.associateBy { it.externalId }

            log.info { "[ConfluenceSync] Found existing documents in RDB: count=${existingDocs.size}" }

            // 3. Convert to vector store documents & Prepare RDB entities (parallel processing)
            log.info { "3. Converting to documents (parallel processing)..." }
            val (vectorDocuments, rdbDocuments) = convertToDocuments(hierarchy, spaceKey, kb, existingDocMap)

            val currentExternalIds = rdbDocuments.map { it.externalId }.toSet()
            log.info { "[ConfluenceSync] Converted documents: count=${vectorDocuments.size} (chunks), pages/pdfs=${rdbDocuments.size}" }

            // 4. Delete removed documents
            val deletedExternalIds = existingExternalIds - currentExternalIds
            if (deletedExternalIds.isNotEmpty()) {
                log.info { "4. Deleting ${deletedExternalIds.size} removed documents..." }
                try {
                    // Delete from RDB
                    withContext(Dispatchers.IO + MDCContext()) {
                        documentRepository.deleteByKnowledgeBaseIdAndExternalIdIn(kb.id!!, deletedExternalIds.toList())
                    }
                    log.info { "[ConfluenceSync] Deleted removed documents from RDB: count=${deletedExternalIds.size}" }
                } catch (e: Exception) {
                    log.warn { "Failed to delete some documents: ${e.message}" }
                }
            } else {
                log.info { "4. No documents to delete" }
            }

            // 5. Store/Update in OpenSearch & RDB (batch processing)
            log.info { "5. Storing/Updating in OpenSearch & RDB..." }

            // Save to RDB
            withContext(Dispatchers.IO + MDCContext()) {
                documentRepository.saveAll(rdbDocuments)
            }
            log.info { "Saved ${rdbDocuments.size} documents to RDB" }

            // Save to Vector Store
            val batchSize = 10
            val batches = vectorDocuments.chunked(batchSize)
            var successCount = 0

            batches.forEachIndexed { batchIndex, batch ->
                try {
                    withContext(Dispatchers.IO + MDCContext()) {
                        vectorStore.add(batch)
                    }
                    successCount += batch.size
                } catch (e: Exception) {
                    log.error(e) { "Failed to add batch ${batchIndex + 1}" }
                }
            }

            log.info { "[ConfluenceSync] Stored/updated documents: total_chunks=$successCount" }

            // Record metrics
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "success")))
        } catch (e: Exception) {
            sample.stop(meterRegistry.timer("task.execution.time", tags.and("status", "failure")))
            log.error(e) { "Error occurred during Confluence sync for KB ${kb.name}: ${e.message}" }
        }
    }

    /**
     * Convert Confluence hierarchy to vector store documents AND RDB entities
     */
    private suspend fun convertToDocuments(
        hierarchy: ContentHierarchy,
        spaceKey: String,
        kb: KnowledgeBase,
        existingDocMap: Map<String, com.okestro.okchat.knowledge.model.entity.Document>
    ): Pair<List<Document>, List<com.okestro.okchat.knowledge.model.entity.Document>> {
        val vectorDocuments = mutableListOf<Document>()
        val rdbDocuments = mutableListOf<com.okestro.okchat.knowledge.model.entity.Document>()

        val allPages = hierarchy.getAllPages()
        val totalPages = allPages.size

        log.info { "Converting $totalPages pages..." }

        val apiCallSemaphore = Semaphore(5)

        val results = coroutineScope {
            allPages.mapIndexed { _, node ->
                async(Dispatchers.IO) {
                    val path = getPagePath(node, hierarchy)
                    val pageTitle = node.title.ifBlank { "Untitled-${node.id}" }
                    val pageContent = node.body?.let { stripHtml(it) } ?: ""

                    // Keyword extraction logic...
                    val keywords = if (pageContent.isBlank()) {
                        emptyList()
                    } else {
                        apiCallSemaphore.withPermit {
                            try {
                                val documentMessage = extractFromDocument(pageContent, pageTitle)
                                documentKeywordExtractionService.execute(documentMessage)
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }

                    val pathKeywords = ContentPath.split(path).map { it.trim() }.filter { it.isNotBlank() }
                    val allKeywords = (keywords + pathKeywords).distinct()

                    val documentContent = pageContent.ifBlank { pageTitle }
                    val wikiBaseUrl = confluenceProperties.baseUrl.removeSuffix("/api/v2").removeSuffix("/")
                    val pageUrl = "$wikiBaseUrl/wiki/spaces/$spaceKey/pages/${node.id}"

                    // RDB Entity (Page)
                    val pageDocId = existingDocMap[node.id]?.id ?: TsidCreator.getTsid().toString()
                    val rdbDoc = com.okestro.okchat.knowledge.model.entity.Document(
                        id = pageDocId,
                        knowledgeBaseId = kb.id!!,
                        externalId = node.id,
                        title = pageTitle,
                        path = path,
                        webUrl = pageUrl,
                        metadata = mapOf(
                            MetadataFields.Nested.TYPE to KnowledgeBaseType.CONFLUENCE.name,
                            MetadataFields.Nested.KEYWORDS to allKeywords,
                            MetadataFields.Nested.SPACE_KEY to spaceKey
                        ),
                        lastSyncedAt = Instant.now()
                    )

                    // Vector Document (Page)
                    val baseMetadata = metadata {
                        this.id = node.id
                        this.title = pageTitle
                        this.type = KnowledgeBaseType.CONFLUENCE.name
                        this.spaceKey = spaceKey
                        this.path = path
                        this.keywords = allKeywords
                        property(MetadataFields.Additional.IS_EMPTY, pageContent.isBlank())
                        property(MetadataFields.Additional.WEB_URL, pageUrl)
                        property("knowledgeBaseId", kb.id)
                    }

                    val baseDocument = Document(
                        node.id,
                        documentContent,
                        baseMetadata.toMap()
                    )

                    // Chunking for Page
                    val chunks = try {
                        chunkingStrategy.chunk(baseDocument)
                    } catch (e: Exception) {
                        listOf(baseDocument)
                    }

                    val finalChunks = if (chunks.size == 1) {
                        chunks
                    } else {
                        chunks.mapIndexed { chunkIndex, chunk ->
                            val chunkMetadata = metadata {
                                this.id = node.id
                                this.keywords = allKeywords
                                property(MetadataFields.Additional.CHUNK_INDEX, chunkIndex)
                                property(MetadataFields.Additional.TOTAL_CHUNKS, chunks.size)
                                property("knowledgeBaseId", kb.id)
                            }
                            Document(
                                "${node.id}_chunk_$chunkIndex",
                                chunk.text ?: "",
                                chunk.metadata + chunkMetadata.toMap()
                            )
                        }
                    }.toMutableList()

                    // PDF Processing
                    val pdfRdbDocs = mutableListOf<com.okestro.okchat.knowledge.model.entity.Document>()
                    try {
                        val pdfDocuments = pdfAttachmentService.processPdfAttachments(node.id, pageTitle, spaceKey, path)
                        if (pdfDocuments.isNotEmpty()) {
                            pdfDocuments.forEach { pdfDoc ->
                                val pdfChunks = try {
                                    chunkingStrategy.chunk(pdfDoc)
                                } catch (e: Exception) {
                                    listOf(pdfDoc)
                                }

                                val finalPdfChunks = if (pdfChunks.size == 1) {
                                    pdfChunks
                                } else {
                                    pdfChunks.mapIndexed { i, chunk ->
                                        Document(
                                            "${pdfDoc.id}_chunk_$i",
                                            chunk.text ?: "",
                                            chunk.metadata + mapOf(
                                                MetadataFields.Additional.CHUNK_INDEX to i,
                                                MetadataFields.Additional.TOTAL_CHUNKS to pdfChunks.size
                                            )
                                        )
                                    }
                                }
                                finalChunks.addAll(finalPdfChunks)

                                // Create RDB entity for PDF - Handle ID reuse
                                val pdfDocId = existingDocMap[pdfDoc.id]?.id ?: TsidCreator.getTsid().toString()

                                pdfRdbDocs.add(
                                    com.okestro.okchat.knowledge.model.entity.Document(
                                        id = pdfDocId,
                                        knowledgeBaseId = kb.id,
                                        externalId = pdfDoc.id,
                                        title = pdfDoc.metadata["title"] as? String ?: "PDF Attachment",
                                        path = pdfDoc.metadata["path"] as? String ?: path,
                                        webUrl = pdfDoc.metadata[MetadataFields.Additional.WEB_URL] as? String,
                                        metadata = pdfDoc.metadata,
                                        lastSyncedAt = Instant.now()
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        log.warn(e) { "Failed to process PDFs for page $pageTitle" }
                    }

                    Pair(finalChunks, listOf(rdbDoc) + pdfRdbDocs)
                }
            }.awaitAll()
        }

        results.forEach { (chunks, rdbDocs) ->
            vectorDocuments.addAll(chunks)
            rdbDocuments.addAll(rdbDocs)
        }

        return Pair(vectorDocuments, rdbDocuments)
    }

    private suspend fun extractFromDocument(content: String, title: String? = null): String {
        return """
            Title: ${title ?: "N/A"}
            Content: ${content.take(2000)}...
        """.trimIndent()
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<.*?>"), " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun getPagePath(node: ContentNode, hierarchy: ContentHierarchy): String {
        val pathNodes = hierarchy.getPathToNode(node.id) ?: return node.title
        return ContentPath.of(pathNodes.map { it.title })
    }
}
