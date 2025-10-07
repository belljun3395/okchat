package com.okestro.okchat.search.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocumentMetadataTest {

    @Test
    fun `fromMap should convert map to DocumentMetadata`() {
        // Given
        val map = mapOf(
            "title" to "Test Title",
            "path" to "/test/path",
            "spaceKey" to "TEST",
            "keywords" to "test, keywords",
            "id" to "123",
            "type" to "page"
        )

        // When
        val metadata = DocumentMetadata.fromMap(map)

        // Then
        assertEquals("Test Title", metadata.title)
        assertEquals("/test/path", metadata.path)
        assertEquals("TEST", metadata.spaceKey)
        assertEquals("test, keywords", metadata.keywords)
        assertEquals("123", metadata.id)
        assertEquals("page", metadata.type)
    }

    @Test
    fun `fromMap should handle missing values`() {
        // Given
        val map = mapOf("title" to "Test Title")

        // When
        val metadata = DocumentMetadata.fromMap(map)

        // Then
        assertEquals("Test Title", metadata.title)
        assertNull(metadata.path)
        assertNull(metadata.spaceKey)
    }

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
}
