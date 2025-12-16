package com.okestro.okchat.search.model

import com.okestro.okchat.search.index.DocumentIndex
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DocumentMetadataTest {

    @Test
    fun `toMap should convert DocumentMetadata to map`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            path = "/test/path",
            spaceKey = "TEST",
            keywords = "test, keywords",
            id = "123",
            type = "page"
        )

        // When
        val map = metadata.toMap()

        // Then
        assertEquals("Test Title", map["title"])
        assertEquals("/test/path", map["path"])
        assertEquals("TEST", map["spaceKey"])
        assertEquals("test, keywords", map["keywords"])
        assertEquals("123", map["id"])
        assertEquals("page", map["type"])
    }

    @Test
    fun `toMap should filter null values`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            path = null,
            spaceKey = null
        )

        // When
        val map = metadata.toMap()

        // Then
        assertEquals(1, map.size)
        assertEquals("Test Title", map["title"])
    }

    @Test
    fun `toMap should include additional properties`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            knowledgeBaseId = 999L,
            isEmpty = false
        )

        // When
        val map = metadata.toMap()

        // Then
        assertEquals(3, map.size)
        assertEquals("Test Title", map["title"])
        assertEquals(999L, map["knowledgeBaseId"])
        assertEquals(false, map["isEmpty"])
    }

    @Test
    fun `toFlatMap should convert to flat structure with metadata prefix`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            path = "/test/path",
            spaceKey = "TEST",
            keywords = "test, keywords",
            id = "123",
            type = "confluence-page"
        )

        // When
        val flatMap = metadata.toFlatMap()

        // Then
        assertEquals("Test Title", flatMap[DocumentIndex.DocumentCommonMetadata.TITLE.fullKey])
        assertEquals("/test/path", flatMap[DocumentIndex.DocumentCommonMetadata.PATH.fullKey])
        assertEquals("TEST", flatMap[DocumentIndex.DocumentCommonMetadata.SPACE_KEY.fullKey])
        assertEquals("test, keywords", flatMap[DocumentIndex.DocumentCommonMetadata.KEYWORDS.fullKey])
        assertEquals("123", flatMap[DocumentIndex.DocumentCommonMetadata.ID.fullKey])
        assertEquals("confluence-page", flatMap[DocumentIndex.DocumentCommonMetadata.TYPE.fullKey])
    }

    @Test
    fun `toFlatMap should include additional properties with metadata prefix`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            webUrl = "http://example.com",
            chunkIndex = 0
        )

        // When
        val flatMap = metadata.toFlatMap()

        // Then
        assertEquals("Test Title", flatMap[DocumentIndex.DocumentCommonMetadata.TITLE.fullKey])
        assertEquals("http://example.com", flatMap[DocumentIndex.DocumentCommonMetadata.WEB_URL.fullKey])
        assertEquals(0, flatMap["metadata.chunkIndex"])
    }

    @Test
    fun `fromFlatMap should create DocumentMetadata from flat structure`() {
        // Given
        val map = mapOf(
            DocumentIndex.DocumentCommonMetadata.TITLE.fullKey to "Test Title",
            DocumentIndex.DocumentCommonMetadata.PATH.fullKey to "/test/path",
            DocumentIndex.DocumentCommonMetadata.SPACE_KEY.fullKey to "TEST",
            DocumentIndex.DocumentCommonMetadata.KEYWORDS.fullKey to "test, keywords",
            DocumentIndex.DocumentCommonMetadata.ID.fullKey to "123",
            DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.fullKey to 999
        )

        // When
        val metadata = DocumentMetadata.fromFlatMap(map)

        // Then
        assertEquals("Test Title", metadata.title)
        assertEquals("/test/path", metadata.path)
        assertEquals("TEST", metadata.spaceKey)
        assertEquals("test, keywords", metadata.keywords)
        assertEquals("123", metadata.id)
        assertEquals(999L, metadata.knowledgeBaseId)
    }

    @Test
    fun `fromFlatMap should handle PDF fields`() {
        // Given
        val map = mapOf(
            DocumentIndex.AttachmentMetadata.PAGE_ID.fullKey to "page-123",
            DocumentIndex.AttachmentMetadata.CHUNK_INDEX.fullKey to 5,
            DocumentIndex.AttachmentMetadata.TOTAL_CHUNKS.fullKey to 10
        )

        // When
        val metadata = DocumentMetadata.fromFlatMap(map)

        // Then
        assertEquals("page-123", metadata.pageId)
        assertEquals(5, metadata.chunkIndex)
        assertEquals(10, metadata.totalChunks)
    }
}
