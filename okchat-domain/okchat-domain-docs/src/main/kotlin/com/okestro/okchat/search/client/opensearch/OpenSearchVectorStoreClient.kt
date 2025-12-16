package com.okestro.okchat.search.client.opensearch

import com.okestro.okchat.search.index.DocumentIndex
import com.okestro.okchat.search.model.SearchDocument
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * OpenSearch implementation of Spring AI VectorStore.
 *
 * Responsibilities:
 * - Document storage with embeddings
 * - Vector similarity search
 * - Document deletion
 *
 * This class follows Single Responsibility Principle by focusing
 * solely on vector store operations.
 */
@Component
class OpenSearchVectorStoreClient(
    private val client: OpenSearchClient,
    private val embeddingModel: EmbeddingModel,
    @Value("\${spring.ai.vectorstore.opensearch.index-name:${DocumentIndex.INDEX_NAME}}") private val indexName: String
) : VectorStore {

    override fun add(documents: List<Document>) {
        log.info { "[VectorStore] Adding ${documents.size} documents" }

        val errors = mutableListOf<Pair<String, Exception>>()
        var successCount = 0

        documents.forEach { document ->
            try {
                val docText = document.text ?: ""

                // Generate embedding
                val embedding = try {
                    embeddingModel.embed(docText).toList()
                } catch (e: Exception) {
                    log.error { "[VectorStore] Embedding failed: ${document.id}, length=${docText.length}" }
                    throw e
                }

                // Create document with embedding (flatten metadata for better search)
                val docMap = buildMap<String, Any> {
                    put(DocumentIndex.Fields.ID, document.id)
                    put(DocumentIndex.Fields.CONTENT, docText)
                    put(DocumentIndex.Fields.EMBEDDING, embedding)

                    // Flatten metadata fields with dot notation for easier querying
                    document.metadata.forEach { (key, value) ->
                        put("metadata.$key", value)
                    }
                }.toMutableMap()

                // Index document
                client.index { idx ->
                    idx.index(indexName)
                        .id(document.id)
                        .document(docMap)
                }

                successCount++
            } catch (e: Exception) {
                log.error { "[VectorStore] Failed to index: ${document.id}" }
                errors.add(document.id to e)
            }
        }

        log.info { "[VectorStore] Indexed $successCount/${documents.size} documents" }

        // If any errors occurred, log summary
        if (errors.isNotEmpty()) {
            log.warn { "[VectorStore] ${errors.size} documents failed" }
            errors.take(3).forEach { (id, ex) ->
                log.error { "[VectorStore] Failed: $id - ${ex.message}" }
            }
            if (errors.size > 3) {
                log.error { "[VectorStore] ... and ${errors.size - 3} more failures" }
            }
        }
    }

    override fun delete(documentIds: List<String>) {
        log.info { "[OpenSearch VectorStore] Deleting ${documentIds.size} documents from index: $indexName" }

        try {
            documentIds.forEach { id ->
                client.delete { del ->
                    del.index(indexName).id(id)
                }
                log.debug { "[OpenSearch VectorStore] Deleted document: $id" }
            }
        } catch (e: Exception) {
            log.error(e) { "[OpenSearch VectorStore] Failed to delete documents" }
            throw e
        }
    }

    override fun delete(filterExpression: Filter.Expression) {
        log.warn { "[OpenSearch VectorStore] Filter-based deletion not yet implemented" }
        // TODO: Implement filter-based deletion if needed
    }

    override fun similaritySearch(request: SearchRequest): List<Document> {
        log.info { "[OpenSearch VectorStore] Similarity search: query='${request.query}', k=${request.topK}" }

        try {
            // Simplified: Use keyword search only for now
            // TODO: Add k-NN vector search support
            val searchResponse = client.search({ search ->
                search.index(indexName)
                    .size(request.topK)
                    .query { q ->
                        q.match { m ->
                            m.field(DocumentIndex.Fields.CONTENT)
                                .query(FieldValue.of(request.query))
                        }
                    }
            }, Map::class.java)

            // Convert results to documents
            val documents = searchResponse.hits().hits().mapNotNull { hit ->
                val source = hit.source() ?: return@mapNotNull null

                // Convert to type-safe SearchDocument
                val searchDoc = SearchDocument.fromMap(source)

                val id = searchDoc.id.ifEmpty { hit.id() ?: return@mapNotNull null }
                val content = searchDoc.content
                val metadata = searchDoc.metadata.toMap()

                Document(id, content, metadata)
            }

            log.info { "[OpenSearch VectorStore] Found ${documents.size} similar documents" }
            return documents
        } catch (e: Exception) {
            log.error(e) { "[OpenSearch VectorStore] Similarity search failed" }
            throw e
        }
    }
}
