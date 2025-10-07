package com.okestro.okchat.search.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SearchDocumentTest {

    @Test
    fun `getActualPageId should extract page ID from chunked document ID`() {
        // Given
        val chunkedDoc = SearchDocument(id = "page123_chunk_0")
        val regularDoc = SearchDocument(id = "page456")

        // Then
        assertEquals("page123", chunkedDoc.getActualPageId())
        assertEquals("page456", regularDoc.getActualPageId())
    }

    @Test
    fun `getTitle should prefer flattened metadata over nested`() {
        // Given
        val docWithFlattenedTitle = SearchDocument(
            metadataTitle = "Flattened Title",
            metadata = DocumentMetadata(title = "Nested Title")
        )
        val docWithNestedTitle = SearchDocument(
            metadata = DocumentMetadata(title = "Nested Title")
        )
        val docWithoutTitle = SearchDocument()

        // Then
        assertEquals("Flattened Title", docWithFlattenedTitle.getTitle())
        assertEquals("Nested Title", docWithNestedTitle.getTitle())
        assertEquals("Untitled", docWithoutTitle.getTitle())
    }

    @Test
    fun `getPath should prefer flattened metadata over nested`() {
        // Given
        val docWithFlattenedPath = SearchDocument(
            metadataPath = "/flattened/path",
            metadata = DocumentMetadata(path = "/nested/path")
        )
        val docWithNestedPath = SearchDocument(
            metadata = DocumentMetadata(path = "/nested/path")
        )
        val docWithoutPath = SearchDocument()

        // Then
        assertEquals("/flattened/path", docWithFlattenedPath.getPath())
        assertEquals("/nested/path", docWithNestedPath.getPath())
        assertEquals("", docWithoutPath.getPath())
    }

    @Test
    fun `getSpaceKey should prefer flattened metadata over nested`() {
        // Given
        val docWithFlattenedKey = SearchDocument(
            metadataSpaceKey = "FLAT",
            metadata = DocumentMetadata(spaceKey = "NEST")
        )
        val docWithNestedKey = SearchDocument(
            metadata = DocumentMetadata(spaceKey = "NEST")
        )
        val docWithoutKey = SearchDocument()

        // Then
        assertEquals("FLAT", docWithFlattenedKey.getSpaceKey())
        assertEquals("NEST", docWithNestedKey.getSpaceKey())
        assertEquals("", docWithoutKey.getSpaceKey())
    }

    @Test
    fun `fromMap should parse flattened structure`() {
        // Given
        val map = mapOf(
            "id" to "doc123",
            "content" to "test content",
            "metadata.title" to "Test Title",
            "metadata.path" to "/test/path",
            "metadata.spaceKey" to "TEST"
        )

        // When
        val doc = SearchDocument.fromMap(map)

        // Then
        assertEquals("doc123", doc.id)
        assertEquals("test content", doc.content)
        assertEquals("Test Title", doc.getTitle())
        assertEquals("/test/path", doc.getPath())
        assertEquals("TEST", doc.getSpaceKey())
    }

    @Test
    fun `fromMap should parse nested structure`() {
        // Given
        val map = mapOf(
            "id" to "doc123",
            "content" to "test content",
            "metadata" to mapOf(
                "title" to "Test Title",
                "path" to "/test/path",
                "spaceKey" to "TEST"
            )
        )

        // When
        val doc = SearchDocument.fromMap(map)

        // Then
        assertEquals("doc123", doc.id)
        assertEquals("test content", doc.content)
        assertEquals("Test Title", doc.getTitle())
        assertEquals("/test/path", doc.getPath())
        assertEquals("TEST", doc.getSpaceKey())
    }

    @Test
    fun `fromMap should handle empty map gracefully`() {
        // Given
        val emptyMap = emptyMap<String, Any>()

        // When
        val doc = SearchDocument.fromMap(emptyMap)

        // Then
        assertEquals("", doc.id)
        assertEquals("", doc.content)
        assertEquals("Untitled", doc.getTitle())
    }
}
