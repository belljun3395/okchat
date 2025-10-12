package com.okestro.okchat.confluence.service

import com.okestro.okchat.confluence.client.Attachment
import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.config.ConfluenceProperties
import com.okestro.okchat.search.support.MetadataFields
import com.okestro.okchat.search.support.metadata
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.ai.document.Document
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

private val log = KotlinLogging.logger {}

/**
 * Service for handling PDF attachments from Confluence
 * Downloads PDF files and extracts text content using Spring AI PDF Reader
 */
@Service
class PdfAttachmentService(
    private val confluenceClient: ConfluenceClient,
    private val confluenceProperties: ConfluenceProperties,
    @Qualifier("confluenceWebClient")
    private val webClient: WebClient,
    private val exchangeStrategies: ExchangeStrategies
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
     * Download attachment file using downloadLink from API response
     */
    private suspend fun downloadAttachmentFile(attachment: Attachment): ByteArray {
        return withContext(Dispatchers.IO) {
            // Use downloadLink from API response
            val downloadLink = attachment.downloadLink ?: attachment._links?.download
                ?: throw IllegalStateException("No download link available for attachment: ${attachment.id}")

            // Construct full URL: wiki base URL + download link
            // baseUrl is like "https://okestro.atlassian.net/wiki/api/v2"
            // downloadLink is like "/download/attachments/2164261392/file.pdf?..." (already URL-encoded)
            val wikiBaseUrl = confluenceProperties.baseUrl.removeSuffix("/api/v2").removeSuffix("/")
            val fullUrl = "$wikiBaseUrl$downloadLink"

            log.debug { "[PdfAttachment] Downloading from URL: $fullUrl" }

            // Create URI directly to avoid double encoding
            // downloadLink is already encoded by Confluence API, so we use it as-is
            val uri = URI.create(fullUrl)

            try {
                // Download file using WebClient (auth headers already configured in bean)
                // Handle redirects manually since Confluence returns 302 to media.atlassian.com
                val responseEntity = webClient.get()
                    .uri(uri)
                    .exchangeToMono { response ->
                        log.debug { "[PdfAttachment] Response status: ${response.statusCode()}" }

                        if (response.statusCode().is3xxRedirection) {
                            // Handle redirect manually
                            val location = response.headers().asHttpHeaders().location
                            log.debug { "[PdfAttachment] Following redirect to: $location" }

                            if (location != null) {
                                // Create a new WebClient without auth headers but with same buffer size config
                                WebClient.builder()
                                    .exchangeStrategies(exchangeStrategies)
                                    .build()
                                    .get()
                                    .uri(location)
                                    .retrieve()
                                    .toEntity(ByteArray::class.java)
                            } else {
                                throw IllegalStateException("Redirect response without Location header for attachment: ${attachment.id}")
                            }
                        } else {
                            response.toEntity(ByteArray::class.java)
                        }
                    }
                    .awaitSingle()

                log.debug { "[PdfAttachment] Download completed: status=${responseEntity.statusCode}, body size=${responseEntity.body?.size ?: 0}" }

                responseEntity.body ?: throw IllegalStateException("Empty response body for attachment: ${attachment.id}")
            } catch (e: NoSuchElementException) {
                log.error(e) { "[PdfAttachment] Empty response from server for URL: $fullUrl" }
                throw IllegalStateException("No content received for attachment: ${attachment.id}", e)
            } catch (e: Exception) {
                log.error(e) { "[PdfAttachment] Failed to download PDF from URL: $fullUrl" }
                throw e
            }
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

                // Download PDF binary data using downloadLink from API response
                val pdfBytes = downloadAttachmentFile(attachment)

                // Create ByteArrayResource for Spring AI PDF Reader
                val resource = ByteArrayResource(pdfBytes)

                // Use Spring AI PDF Reader to extract text (with default config)
                val pdfReader = PagePdfDocumentReader(resource)
                val extractedDocuments = pdfReader.get()

                log.info { "[PdfAttachment] Extracted ${extractedDocuments.size} page(s) from PDF: ${attachment.title}" }

                // Merge all pages into a single document for better context
                val fullText = extractedDocuments.mapIndexed { index, doc ->
                    "=== Page ${index + 1} ===\n${doc.text ?: ""}"
                }.joinToString("\n\n")

                log.info { "[PdfAttachment] Merged PDF content: ${fullText.length} chars total" }

                // Create single document with all PDF content
                val wikiBaseUrl = confluenceProperties.baseUrl.removeSuffix("/api/v2").removeSuffix("/")
                val pdfMetadata = metadata {
                    this.id = attachment.id
                    this.title = "$pageTitle-${attachment.title}"
                    this.type = "confluence-pdf-attachment"
                    this.spaceKey = spaceKey
                    this.path = "$path > ${attachment.title}"

                    // Additional PDF-specific properties
                    property(MetadataFields.Additional.PAGE_ID, attachment.pageId ?: "")
                    property(MetadataFields.Additional.ATTACHMENT_TITLE, attachment.title)
                    property(MetadataFields.Additional.TOTAL_PDF_PAGES, extractedDocuments.size)
                    property(MetadataFields.Additional.FILE_SIZE, attachment.fileSize ?: 0)
                    property(MetadataFields.Additional.MEDIA_TYPE, attachment.mediaType)

                    // Links for user access
                    property(MetadataFields.Additional.DOWNLOAD_URL, attachment.downloadLink?.let { "$wikiBaseUrl$it" } ?: "")
                    property(MetadataFields.Additional.WEB_URL, attachment.webuiLink?.let { "$wikiBaseUrl$it" } ?: "")
                }

                listOf(
                    Document(
                        attachment.id,
                        fullText,
                        pdfMetadata.toMap()
                    )
                )
            } catch (e: Exception) {
                log.error(e) { "[PdfAttachment] Failed to extract text from PDF: ${attachment.title} (id=${attachment.id}), error=${e.message}" }
                emptyList()
            }
        }
    }
}
