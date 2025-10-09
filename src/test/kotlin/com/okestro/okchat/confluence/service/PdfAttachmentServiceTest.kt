package com.okestro.okchat.confluence.service

import com.okestro.okchat.search.model.MetadataFields
import com.okestro.okchat.search.model.metadata
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PdfAttachmentServiceTest {

    @Test
    fun `PDF metadata should be created with DSL`() {
        // Given
        val attachmentId = "att123"
        val pageTitle = "Technical Documentation"
        val attachmentTitle = "Architecture.pdf"
        val spaceKey = "TECH"
        val path = "Documentation > Technical"
        val pageId = "page456"
        val index = 0
        val totalPages = 5

        // When
        val pdfMetadata = metadata {
            this.id = attachmentId
            this.title = "$pageTitle - $attachmentTitle (Page ${index + 1})"
            this.type = "confluence-pdf-attachment"
            this.spaceKey = spaceKey
            this.path = "$path > $attachmentTitle"
            
            // Additional PDF-specific properties
            "pageId" to pageId
            "attachmentTitle" to attachmentTitle
            "pdfPageNumber" to (index + 1)
            "totalPdfPages" to totalPages
            "fileSize" to 1024000L
            "mediaType" to "application/pdf"
        }

        // Then
        assertEquals(attachmentId, pdfMetadata.id)
        assertEquals("$pageTitle - $attachmentTitle (Page 1)", pdfMetadata.title)
        assertEquals("confluence-pdf-attachment", pdfMetadata.type)
        assertEquals(spaceKey, pdfMetadata.spaceKey)
        assertEquals("$path > $attachmentTitle", pdfMetadata.path)
        
        // Check additional properties
        assertEquals(pageId, pdfMetadata.additionalProperties["pageId"])
        assertEquals(attachmentTitle, pdfMetadata.additionalProperties["attachmentTitle"])
        assertEquals(1, pdfMetadata.additionalProperties["pdfPageNumber"])
        assertEquals(totalPages, pdfMetadata.additionalProperties["totalPdfPages"])
        assertEquals(1024000L, pdfMetadata.additionalProperties["fileSize"])
        assertEquals("application/pdf", pdfMetadata.additionalProperties["mediaType"])
    }

    @Test
    fun `PDF metadata should convert to flat map correctly`() {
        // Given
        val pdfMetadata = metadata {
            this.id = "att123"
            this.title = "Test PDF Page 1"
            this.type = "confluence-pdf-attachment"
            this.spaceKey = "TEST"
            this.path = "Test > PDF"
            
            "pageId" to "page456"
            "attachmentTitle" to "test.pdf"
            "pdfPageNumber" to 1
            "totalPdfPages" to 3
            "fileSize" to 512000L
            "mediaType" to "application/pdf"
        }

        // When
        val flatMap = pdfMetadata.toFlatMap()

        // Then
        assertEquals("att123", flatMap[MetadataFields.ID])
        assertEquals("Test PDF Page 1", flatMap[MetadataFields.TITLE])
        assertEquals("confluence-pdf-attachment", flatMap[MetadataFields.TYPE])
        assertEquals("TEST", flatMap[MetadataFields.SPACE_KEY])
        assertEquals("Test > PDF", flatMap[MetadataFields.PATH])
        
        // Check flattened additional properties
        assertEquals("page456", flatMap["metadata.pageId"])
        assertEquals("test.pdf", flatMap["metadata.attachmentTitle"])
        assertEquals(1, flatMap["metadata.pdfPageNumber"])
        assertEquals(3, flatMap["metadata.totalPdfPages"])
        assertEquals(512000L, flatMap["metadata.fileSize"])
        assertEquals("application/pdf", flatMap["metadata.mediaType"])
    }

    @Test
    fun `PDF metadata should support multiple pages`() {
        // Given
        val attachmentId = "att999"
        val pageTitle = "API Documentation"
        val attachmentTitle = "API-Guide.pdf"
        val totalPages = 10

        // When - Create metadata for each page
        val metadataList = (0 until totalPages).map { index ->
            metadata {
                this.id = attachmentId
                this.title = "$pageTitle - $attachmentTitle (Page ${index + 1})"
                this.type = "confluence-pdf-attachment"
                
                "pdfPageNumber" to (index + 1)
                "totalPdfPages" to totalPages
            }
        }

        // Then
        assertEquals(10, metadataList.size)
        metadataList.forEachIndexed { index, meta ->
            assertEquals("$pageTitle - $attachmentTitle (Page ${index + 1})", meta.title)
            assertEquals(index + 1, meta.additionalProperties["pdfPageNumber"])
            assertEquals(totalPages, meta.additionalProperties["totalPdfPages"])
        }
    }

    @Test
    fun `PDF metadata should handle large file sizes`() {
        // Given
        val largeFileSize = 50 * 1024 * 1024L // 50MB

        // When
        val pdfMetadata = metadata {
            this.id = "large-att"
            this.title = "Large PDF Document"
            this.type = "confluence-pdf-attachment"
            
            "fileSize" to largeFileSize
            "mediaType" to "application/pdf"
        }

        // Then
        assertEquals(largeFileSize, pdfMetadata.additionalProperties["fileSize"])
    }

    @Test
    fun `PDF metadata should handle empty or null values`() {
        // Given
        val emptyPageId = ""

        // When
        val pdfMetadata = metadata {
            this.id = "att123"
            this.title = "Test PDF"
            this.type = "confluence-pdf-attachment"
            
            "pageId" to emptyPageId
            "attachmentTitle" to "test.pdf"
        }

        // Then
        assertEquals("", pdfMetadata.additionalProperties["pageId"])
        assertNotNull(pdfMetadata.additionalProperties["attachmentTitle"])
    }

    @Test
    fun `PDF metadata should support path hierarchies`() {
        // Given
        val complexPath = "Workspace > Projects > Backend > Documentation"
        val attachmentTitle = "Database-Schema.pdf"

        // When
        val pdfMetadata = metadata {
            this.id = "att-db"
            this.title = "Database Schema Documentation"
            this.path = "$complexPath > $attachmentTitle"
            this.type = "confluence-pdf-attachment"
        }

        // Then
        assertTrue(pdfMetadata.path!!.contains(">"))
        assertTrue(pdfMetadata.path!!.endsWith(attachmentTitle))
        assertEquals("$complexPath > $attachmentTitle", pdfMetadata.path)
    }

    @Test
    fun `PDF metadata DSL should be readable and maintainable`() {
        // This test demonstrates the readability improvement over map-based approach
        
        // Old approach (for comparison)
        val oldStyleMap = mapOf(
            "id" to "att123",
            "title" to "Document - file.pdf (Page 1)",
            "type" to "confluence-pdf-attachment",
            "spaceKey" to "DOC",
            "path" to "Path > file.pdf",
            "pageId" to "page456",
            "attachmentTitle" to "file.pdf",
            "pdfPageNumber" to 1,
            "totalPdfPages" to 5,
            "fileSize" to 1024000L,
            "mediaType" to "application/pdf"
        )

        // New DSL approach
        val newStyleMetadata = metadata {
            this.id = "att123"
            this.title = "Document - file.pdf (Page 1)"
            this.type = "confluence-pdf-attachment"
            this.spaceKey = "DOC"
            this.path = "Path > file.pdf"
            
            "pageId" to "page456"
            "attachmentTitle" to "file.pdf"
            "pdfPageNumber" to 1
            "totalPdfPages" to 5
            "fileSize" to 1024000L
            "mediaType" to "application/pdf"
        }

        // Both should produce equivalent results
        val flatMap = newStyleMetadata.toFlatMap()
        
        // Verify they contain the same information
        assertEquals(oldStyleMap["id"], newStyleMetadata.id)
        assertEquals(oldStyleMap["title"], newStyleMetadata.title)
        assertEquals(oldStyleMap["type"], newStyleMetadata.type)
        assertEquals(oldStyleMap["spaceKey"], newStyleMetadata.spaceKey)
        assertEquals(oldStyleMap["path"], newStyleMetadata.path)
    }

    @Test
    fun `PDF metadata should support dynamic values`() {
        // Given
        fun calculatePageNumber(index: Int) = index + 1
        fun formatTitle(base: String, attachment: String, page: Int) = 
            "$base - $attachment (Page $page)"

        // When
        val index = 2
        val pdfMetadata = metadata {
            this.id = "att-dynamic"
            this.title = formatTitle("Documentation", "guide.pdf", calculatePageNumber(index))
            this.type = "confluence-pdf-attachment"
            
            "pdfPageNumber" to calculatePageNumber(index)
            "computedValue" to (index * 100)
        }

        // Then
        assertEquals("Documentation - guide.pdf (Page 3)", pdfMetadata.title)
        assertEquals(3, pdfMetadata.additionalProperties["pdfPageNumber"])
        assertEquals(200, pdfMetadata.additionalProperties["computedValue"])
    }
}
