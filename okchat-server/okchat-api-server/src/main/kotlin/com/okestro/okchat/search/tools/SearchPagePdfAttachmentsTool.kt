package com.okestro.okchat.search.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.dto.ToolOutput
import com.okestro.okchat.ai.tools.ToolExecutor
import com.okestro.okchat.search.support.MetadataFields
import com.okestro.okchat.search.tools.dto.SearchPagePdfAttachmentsInput
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch.core.SearchResponse
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Component
import kotlin.collections.get

@Component("SearchPagePdfAttachmentsTool")
@Description("Search PDF attachments for a Confluence page from vector store")
class SearchPagePdfAttachmentsTool(
    private val openSearchClient: OpenSearchClient,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition.builder()
            .name("search_page_pdf_attachments")
            .description("Search PDF attachments for a specific Confluence page from the vector store and read their full text content. This retrieves already-indexed PDF documents and returns their complete text. Use this when you need to read PDF documents attached to a Confluence page.")
            .inputSchema(
                """
                {
                  "type": "object",
                  "properties": {
                    "thought": {
                      "type": "string",
                      "description": "The reasoning for using this tool in this specific context."
                    },
                    "pageId": {
                      "type": "string",
                      "description": "The Confluence page ID to get PDF attachments from"
                    }
                  },
                  "required": ["thought", "pageId"]
                }
                """.trimIndent()
            )
            .build()
    }

    override fun call(toolInput: String): String {
        return ToolExecutor.execute(
            toolName = "GetPagePdfAttachmentsTool",
            objectMapper = objectMapper,
            errorThought = "An error occurred while retrieving PDF attachments from vector store."
        ) {
            // Parse input to type-safe object
            val input = objectMapper.readValue(toolInput, SearchPagePdfAttachmentsInput::class.java)
            val thought = input.thought ?: "No thought provided."
            val pageId = input.pageId

            // Query OpenSearch for PDF attachments of this page
            val pdfAttachments = searchPdfAttachments(pageId)

            val answer = buildString {
                append("=== PDF Attachments from Vector Store ===\n\n")
                append("Page ID: $pageId\n")
                append("Total PDF attachments found: ${pdfAttachments.size}\n\n")

                if (pdfAttachments.isEmpty()) {
                    append("No PDF attachments found for this page in the vector store.\n")
                    append("Note: PDF attachments are only available after running the Confluence sync task.")
                } else {
                    pdfAttachments.forEachIndexed { index, pdf ->
                        append("=".repeat(80) + "\n")
                        append("PDF ${index + 1}/${pdfAttachments.size}\n")
                        append("=".repeat(80) + "\n\n")

                        append("Attachment: ${pdf.attachmentTitle}\n")
                        append("Total Pages: ${pdf.totalPages}\n")

                        if (pdf.fileSize > 0) {
                            val sizeInKB = pdf.fileSize / 1024.0
                            val sizeInMB = sizeInKB / 1024.0
                            val formattedSize = when {
                                sizeInMB >= 1.0 -> "%.2f MB".format(sizeInMB)
                                else -> "%.2f KB".format(sizeInKB)
                            }
                            append("File Size: $formattedSize\n")
                        }

                        if (pdf.downloadUrl.isNotBlank()) {
                            append("Download URL: ${pdf.downloadUrl}\n")
                        }

                        if (pdf.webUrl.isNotBlank()) {
                            append("Web URL: ${pdf.webUrl}\n")
                        }

                        append("\n--- Extracted Text Content ---\n\n")
                        append(pdf.content)
                        append("\n\n")
                    }
                }
            }

            ToolOutput(thought = thought, answer = answer)
        }
    }

    /**
     * Search OpenSearch for PDF attachments of a specific page
     */
    private fun searchPdfAttachments(pageId: String): List<PdfAttachment> {
        val results = mutableListOf<PdfAttachment>()

        try {
            // Search with filters: pageId and type = confluence-pdf-attachment
            val searchResponse: SearchResponse<Map<*, *>> = openSearchClient.search({ s ->
                s.index(indexName)
                    .size(100) // Max 100 PDFs per page
                    .query { q ->
                        q.bool { b ->
                            b.must { m ->
                                m.term { t ->
                                    t.field("metadata.${MetadataFields.Additional.PAGE_ID}")
                                        .value(FieldValue.of(pageId))
                                }
                            }
                                .must { m ->
                                    m.term { t ->
                                        t.field(MetadataFields.TYPE)
                                            .value(FieldValue.of("confluence-pdf-attachment"))
                                    }
                                }
                        }
                    }
                    .source { src ->
                        src.fetch(true) // Include full document
                    }
            }, Map::class.java)

            // Parse results
            searchResponse.hits().hits().forEach { hit ->
                val source = hit.source() ?: return@forEach

                val content = source["content"]?.toString() ?: ""
                val attachmentTitle = (source["metadata.${MetadataFields.Additional.ATTACHMENT_TITLE}"] as? String) ?: "Unknown"
                val totalPages = (source["metadata.${MetadataFields.Additional.TOTAL_PDF_PAGES}"] as? Number)?.toInt() ?: 0
                val fileSize = (source["metadata.${MetadataFields.Additional.FILE_SIZE}"] as? Number)?.toLong() ?: 0L
                val downloadUrl = (source["metadata.${MetadataFields.Additional.DOWNLOAD_URL}"] as? String) ?: ""
                val webUrl = (source["metadata.${MetadataFields.Additional.WEB_URL}"] as? String) ?: ""

                results.add(
                    PdfAttachment(
                        content = content,
                        attachmentTitle = attachmentTitle,
                        totalPages = totalPages,
                        fileSize = fileSize,
                        downloadUrl = downloadUrl,
                        webUrl = webUrl
                    )
                )
            }
        } catch (e: Exception) {
            // Log error but return empty list instead of failing
            throw RuntimeException("Failed to search PDF attachments: ${e.message}", e)
        }

        return results
    }

    /**
     * Data class to hold PDF attachment information
     */
    private data class PdfAttachment(
        val content: String,
        val attachmentTitle: String,
        val totalPages: Int,
        val fileSize: Long,
        val downloadUrl: String,
        val webUrl: String
    )
}
