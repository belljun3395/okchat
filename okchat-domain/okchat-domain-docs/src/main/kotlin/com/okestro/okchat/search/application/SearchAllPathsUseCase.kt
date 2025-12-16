package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseIn
import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseOut
import com.okestro.okchat.search.index.DocumentIndex
import com.okestro.okchat.search.model.AllowedKnowledgeBases
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class SearchAllPathsUseCase(
    private val openSearchClient: OpenSearchClient,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) {
    fun execute(useCaseIn: SearchAllPathsUseCaseIn): SearchAllPathsUseCaseOut {
        val paths = mutableSetOf<String>()
        val scope = useCaseIn.allowedKbIds

        // If scope is Subset with empty list, it means user has NO access -> return empty
        if (scope is AllowedKnowledgeBases.Subset && scope.ids.isEmpty()) {
            return SearchAllPathsUseCaseOut(emptyList())
        }

        try {
            var from = 0
            val size = 200

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .query { q ->
                            when (scope) {
                                is AllowedKnowledgeBases.Subset -> {
                                    q.terms { t ->
                                        t.field(DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.fullKey)
                                            .terms { v ->
                                                v.value(scope.ids.map { FieldValue.of(it) })
                                            }
                                    }
                                }
                                AllowedKnowledgeBases.All -> {
                                    q.matchAll { it }
                                }
                            }
                        }
                        .source { src ->
                            src.filter { f ->
                                f.includes(listOf(DocumentIndex.DocumentCommonMetadata.PATH.fullKey))
                            }
                        }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                // Extract paths
                hits.forEach { hit ->
                    val source = hit.source()
                    val path = source?.get(DocumentIndex.DocumentCommonMetadata.PATH.fullKey)?.toString()
                    path?.let {
                        if (it.isNotBlank()) paths.add(it)
                    }
                }

                // Check if there are more pages
                if (hits.size < size) break
                from += size
            }

            log.info { "Found ${paths.size} unique paths from index (filtered=${scope is AllowedKnowledgeBases.Subset})" }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch paths from index: ${e.message}" }
        }

        return SearchAllPathsUseCaseOut(paths.toList().sorted())
    }
}
