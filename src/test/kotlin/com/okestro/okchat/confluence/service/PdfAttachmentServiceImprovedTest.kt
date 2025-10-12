package com.okestro.okchat.confluence.service

import com.okestro.okchat.fixture.TestFixtures
import com.okestro.okchat.search.model.MetadataFields
import com.okestro.okchat.search.model.metadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith

/**
 * Improved test for PDF metadata creation using Kotest.
 * Demonstrates DSL testing and parameterized test cases.
 */
class PdfAttachmentServiceImprovedTest : DescribeSpec({

    describe("PDF Metadata DSL") {

        context("basic metadata creation") {

            it("should create PDF metadata with DSL") {
                // Given
                val attachmentId = TestFixtures.PdfMetadata.ATTACHMENT_ID
                val pageTitle = TestFixtures.PdfMetadata.PAGE_TITLE
                val attachmentTitle = TestFixtures.PdfMetadata.ATTACHMENT_TITLE
                val spaceKey = TestFixtures.PdfMetadata.SPACE_KEY
                val path = TestFixtures.PdfMetadata.PATH
                val pageId = TestFixtures.PdfMetadata.PAGE_ID
                val index = 0
                val totalPages = 5

                // When
                val pdfMetadata = metadata {
                    this.id = attachmentId
                    this.title = "$pageTitle - $attachmentTitle (Page ${index + 1})"
                    this.type = "confluence-pdf-attachment"
                    this.spaceKey = spaceKey
                    this.path = "$path > $attachmentTitle"

                    "pageId" to pageId
                    "attachmentTitle" to attachmentTitle
                    "pdfPageNumber" to (index + 1)
                    "totalPdfPages" to totalPages
                    "fileSize" to TestFixtures.PdfMetadata.FILE_SIZE
                    "mediaType" to TestFixtures.PdfMetadata.MEDIA_TYPE
                }

                // Then
                pdfMetadata.id shouldBe attachmentId
                pdfMetadata.title shouldBe "$pageTitle - $attachmentTitle (Page 1)"
                pdfMetadata.type shouldBe "confluence-pdf-attachment"
                pdfMetadata.spaceKey shouldBe spaceKey
                pdfMetadata.path shouldBe "$path > $attachmentTitle"

                // Check additional properties
                pdfMetadata.additionalProperties["pageId"] shouldBe pageId
                pdfMetadata.additionalProperties["attachmentTitle"] shouldBe attachmentTitle
                pdfMetadata.additionalProperties["pdfPageNumber"] shouldBe 1
                pdfMetadata.additionalProperties["totalPdfPages"] shouldBe totalPages
                pdfMetadata.additionalProperties["fileSize"] shouldBe TestFixtures.PdfMetadata.FILE_SIZE
                pdfMetadata.additionalProperties["mediaType"] shouldBe TestFixtures.PdfMetadata.MEDIA_TYPE
            }
        }

        context("metadata conversion") {

            it("should convert to flat map correctly") {
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
                flatMap[MetadataFields.ID] shouldBe "att123"
                flatMap[MetadataFields.TITLE] shouldBe "Test PDF Page 1"
                flatMap[MetadataFields.TYPE] shouldBe "confluence-pdf-attachment"
                flatMap[MetadataFields.SPACE_KEY] shouldBe "TEST"
                flatMap[MetadataFields.PATH] shouldBe "Test > PDF"

                // Check flattened additional properties
                flatMap["metadata.pageId"] shouldBe "page456"
                flatMap["metadata.attachmentTitle"] shouldBe "test.pdf"
                flatMap["metadata.pdfPageNumber"] shouldBe 1
                flatMap["metadata.totalPdfPages"] shouldBe 3
                flatMap["metadata.fileSize"] shouldBe 512000L
                flatMap["metadata.mediaType"] shouldBe "application/pdf"
            }
        }

        context("multiple pages") {

            it("should support creating metadata for multiple pages") {
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
                metadataList shouldHaveSize 10
                metadataList.forEachIndexed { index, meta ->
                    meta.title shouldBe "$pageTitle - $attachmentTitle (Page ${index + 1})"
                    meta.additionalProperties["pdfPageNumber"] shouldBe (index + 1)
                    meta.additionalProperties["totalPdfPages"] shouldBe totalPages
                }
            }

            data class PageRangeTestCase(
                val description: String,
                val totalPages: Int
            )

            withData(
                nameFn = { it.description },
                PageRangeTestCase("Small PDF (5 pages)", 5),
                PageRangeTestCase("Medium PDF (20 pages)", 20),
                PageRangeTestCase("Large PDF (100 pages)", 100)
            ) { (_, totalPages) ->
                // When
                val metadataList = (0 until totalPages).map { index ->
                    metadata {
                        this.id = "att-test"
                        this.title = "Test (Page ${index + 1})"
                        this.type = "confluence-pdf-attachment"
                        "pdfPageNumber" to (index + 1)
                        "totalPdfPages" to totalPages
                    }
                }

                // Then
                metadataList shouldHaveSize totalPages
                metadataList.first().additionalProperties["pdfPageNumber"] shouldBe 1
                metadataList.last().additionalProperties["pdfPageNumber"] shouldBe totalPages
            }
        }

        context("file size handling") {

            data class FileSizeTestCase(
                val description: String,
                val fileSize: Long,
                val expectedSize: Long
            )

            withData(
                nameFn = { it.description },
                FileSizeTestCase("Small file (100KB)", 100 * 1024L, 102400L),
                FileSizeTestCase("Medium file (5MB)", 5 * 1024 * 1024L, 5242880L),
                FileSizeTestCase("Large file (50MB)", 50 * 1024 * 1024L, 52428800L),
                FileSizeTestCase("Very large file (200MB)", 200 * 1024 * 1024L, 209715200L)
            ) { (_, fileSize, expectedSize) ->
                // When
                val pdfMetadata = metadata {
                    this.id = "large-att"
                    this.title = "Large PDF Document"
                    this.type = "confluence-pdf-attachment"

                    "fileSize" to fileSize
                    "mediaType" to "application/pdf"
                }

                // Then
                pdfMetadata.additionalProperties["fileSize"] shouldBe expectedSize
            }
        }

        context("edge cases") {

            it("should handle empty or null values") {
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
                pdfMetadata.additionalProperties["pageId"] shouldBe ""
                pdfMetadata.additionalProperties["attachmentTitle"].shouldNotBeNull()
            }

            it("should support path hierarchies") {
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
                val path = pdfMetadata.path
                path.shouldNotBeNull()
                path shouldContain ">"
                path shouldEndWith attachmentTitle
                path shouldBe "$complexPath > $attachmentTitle"
            }
        }

        context("DSL readability comparison") {

            it("should demonstrate improved readability over map-based approach") {
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

                // Verify they contain the same information
                newStyleMetadata.id shouldBe oldStyleMap["id"]
                newStyleMetadata.title shouldBe oldStyleMap["title"]
                newStyleMetadata.type shouldBe oldStyleMap["type"]
                newStyleMetadata.spaceKey shouldBe oldStyleMap["spaceKey"]
                newStyleMetadata.path shouldBe oldStyleMap["path"]
            }
        }

        context("dynamic value computation") {

            it("should support dynamic values in metadata") {
                // Given
                fun calculatePageNumber(index: Int) = index + 1
                fun formatTitle(base: String, attachment: String, page: Int) = "$base - $attachment (Page $page)"

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
                pdfMetadata.title shouldBe "Documentation - guide.pdf (Page 3)"
                pdfMetadata.additionalProperties["pdfPageNumber"] shouldBe 3
                pdfMetadata.additionalProperties["computedValue"] shouldBe 200
            }
        }
    }
})
