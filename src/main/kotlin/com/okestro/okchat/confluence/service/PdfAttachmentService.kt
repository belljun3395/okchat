package com.okestro.okchat.confluence.service

import com.okestro.okchat.confluence.client.Attachment
import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.search.model.metadata
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.document.Document
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for handling PDF attachments from Confluence
 * Downloads PDF files and extracts text content using Spring AI PDF Reader
 */
@Service
class PdfAttachmentService(
    private val confluenceClient: ConfluenceClient
) {
    /**
     * Get all PDF attachments for a page and extract their text content
     *
     * @param pageId The Confluence page ID
     * @param pageTitle The page title (for metadata)
     * @param spaceKey The space key (for metadata)
     * @param path The page path (for metadata)
     * @return List of documents extracted from PDF attachments
     */
    suspend fun processPdfAttachments(
        pageId: String,
        pageTitle: String,
        spaceKey: String,
        path: String
    ): List<Document> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all attachments for the page
                val attachments = getPdfAttachments(pageId)

                if (attachments.isEmpty()) {
                    return@withContext emptyList()
                }

                log.info { "[PdfAttachment] Found ${attachments.size} PDF attachment(s) for page: $pageTitle (id=$pageId)" }

                // Process each PDF attachment
                attachments.flatMap { attachment ->
                    try {
                        extractTextFromPdf(attachment, pageTitle, spaceKey, path)
                    } catch (e: Exception) {
                        log.warn(e) { "[PdfAttachment] Failed to extract text from PDF: ${attachment.title} (id=${attachment.id})" }
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                log.warn(e) { "[PdfAttachment] Failed to process attachments for page: $pageTitle (id=$pageId)" }
                emptyList()
            }
        }
    }

    /**
     * Get PDF attachments for a page (with pagination support)
     */
    private suspend fun getPdfAttachments(pageId: String): List<Attachment> {
        return withContext(Dispatchers.IO) {
            val pdfAttachments = mutableListOf<Attachment>()
            var cursor: String? = null

            do {
                val response = confluenceClient.getPageAttachments(pageId, cursor)

                // Filter for PDF files only
                val pdfs = response.results.filter { attachment ->
                    attachment.mediaType.equals("application/pdf", ignoreCase = true)
                }

                pdfAttachments.addAll(pdfs)
                cursor = response._links?.next
            } while (cursor != null)

            pdfAttachments
        }
    }

    /**
     * Download PDF and extract text content
     */
    private suspend fun extractTextFromPdf(
        attachment: Attachment,
        pageTitle: String,
        spaceKey: String,
        path: String
    ): List<Document> {
        return withContext(Dispatchers.IO) {
            try {
                log.info { "[PdfAttachment] Downloading PDF: ${attachment.title} (size=${attachment.fileSize} bytes)" }

                // Download PDF binary data
                val pdfBytes = confluenceClient.downloadAttachment(attachment.id)

                // Create ByteArrayResource for Spring AI PDF Reader
                val resource = ByteArrayResource(pdfBytes)

                // Use Spring AI PDF Reader to extract text (with default config)
                val pdfReader = PagePdfDocumentReader(resource)
                val extractedDocuments = pdfReader.get()

                log.info { "[PdfAttachment] Extracted ${extractedDocuments.size} page(s) from PDF: ${attachment.title}" }

                // Enrich documents with Confluence metadata using DSL
                extractedDocuments.mapIndexed { index, doc ->
                    val pdfMetadata = metadata {
                        this.id = attachment.id
                        this.title = "$pageTitle - ${attachment.title} (Page ${index + 1})"
                        this.type = "confluence-pdf-attachment"
                        this.spaceKey = spaceKey
                        this.path = "$path > ${attachment.title}"
                        
                        // Additional PDF-specific properties
                        "pageId" to (attachment.pageId ?: "")
                        "attachmentTitle" to attachment.title
                        "pdfPageNumber" to (index + 1)
                        "totalPdfPages" to extractedDocuments.size
                        "fileSize" to (attachment.fileSize ?: 0)
                        "mediaType" to attachment.mediaType
                    }
                    
                    Document(
                        "${attachment.id}_page_$index",
                        doc.text ?: "",
                        pdfMetadata.toMap()
                    )
                }
            } catch (e: Exception) {
                log.error(e) { "[PdfAttachment] Failed to extract text from PDF: ${attachment.title} (id=${attachment.id}), error=${e.message}" }
                emptyList()
            }
        }
    }
}
