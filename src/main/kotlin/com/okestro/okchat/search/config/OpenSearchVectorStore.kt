package com.okestro.okchat.search.config

import com.okestro.okchat.search.model.SearchDocument
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter

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
class OpenSearchVectorStore(
    private val client: OpenSearchClient,
    private val embeddingModel: EmbeddingModel,
    private val indexName: String
) : VectorStore {

    override fun add(documents: List<Document>) {
        log.info { "[OpenSearch VectorStore] Adding ${documents.size} documents to index: $indexName" }

        documents.forEach { document ->
            try {
                // Generate embedding
                val docText = document.text ?: ""
                if (docText.isEmpty()) {
                    log.warn { "[OpenSearch VectorStore] Skipping document with empty text: ${document.id}" }
                    return@forEach
                }
                val embedding = embeddingModel.embed(docText).toList()

                // Create document with embedding (flatten metadata for better search)
                val docMap = mutableMapOf(
                    "id" to document.id,
                    "content" to docText,
                    "embedding" to embedding
                )

                // Flatten metadata fields with dot notation for easier querying
                document.metadata.forEach { (key, value) ->
                    docMap["metadata.$key"] = value
                }

                // Index document
                client.index { idx ->
                    idx.index(indexName)
                        .id(document.id)
                        .document(docMap)
                }

                log.debug { "[OpenSearch VectorStore] Indexed document: ${document.id}" }
            } catch (e: Exception) {
                log.error(e) { "[OpenSearch VectorStore] Failed to index document: ${document.id}" }
                throw e
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
                            m.field("content")
                                .query(org.opensearch.client.opensearch._types.FieldValue.of(request.query))
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
