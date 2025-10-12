package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.SearchAllByPathUseCaseIn
import com.okestro.okchat.search.application.dto.SearchAllByPathUseCaseOut
import com.okestro.okchat.search.model.Document
import com.okestro.okchat.search.model.MetadataFields
import com.okestro.okchat.search.util.extractChunk
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class SearchAllByPathUseCase(
    private val openSearchClient: OpenSearchClient,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) {
    suspend fun execute(useCaseIn: SearchAllByPathUseCaseIn): SearchAllByPathUseCaseOut = withContext(Dispatchers.IO) {
        val documents = mutableMapOf<String, Document>() // Use map to handle chunks and keep unique docs
        val documentPath = useCaseIn.documentPath

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
                                        t.field(MetadataFields.PATH)
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
                                        MetadataFields.ID,
                                        MetadataFields.TITLE,
                                        MetadataFields.PATH
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
                        val confluenceId = source[MetadataFields.ID]?.toString()

                        if (confluenceId != null) {
                            val baseId = confluenceId.extractChunk()

                            if (!documents.containsKey(baseId)) {
                                val title = source[MetadataFields.TITLE]?.toString() ?: "Untitled"
                                val path = source[MetadataFields.PATH]?.toString() ?: ""

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

        SearchAllByPathUseCaseOut(documents.values.toList().sortedBy { it.title })
    }
}
