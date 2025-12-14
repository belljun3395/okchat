package com.okestro.okchat.search.model

import com.okestro.okchat.search.support.MetadataFields
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
    fun `fromMap should handle additional properties`() {
        // Given
        val map = mapOf(
            "title" to "Test Title",
            "customField1" to "value1",
            "customField2" to 42,
            "customField3" to true
        )

        // When
        val metadata = DocumentMetadata.fromMap(map)

        // Then
        assertEquals("Test Title", metadata.title)
        assertEquals(3, metadata.additionalProperties.size)
        assertEquals("value1", metadata.additionalProperties["customField1"])
        assertEquals(42, metadata.additionalProperties["customField2"])
        assertEquals(true, metadata.additionalProperties["customField3"])
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

    @Test
    fun `toMap should include additional properties`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            additionalProperties = mapOf(
                "customField" to "customValue",
                "isEmpty" to false
            )
        )

        // When
        val map = metadata.toMap()

        // Then
        assertEquals(3, map.size)
        assertEquals("Test Title", map["title"])
        assertEquals("customValue", map["customField"])
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
        assertEquals("Test Title", flatMap[MetadataFields.TITLE])
        assertEquals("/test/path", flatMap[MetadataFields.PATH])
        assertEquals("TEST", flatMap[MetadataFields.SPACE_KEY])
        assertEquals("test, keywords", flatMap[MetadataFields.KEYWORDS])
        assertEquals("123", flatMap[MetadataFields.ID])
        assertEquals("confluence-page", flatMap[MetadataFields.TYPE])
    }

    @Test
    fun `toFlatMap should include additional properties with metadata prefix`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            additionalProperties = mapOf(
                "customField" to "customValue",
                "chunkIndex" to 0
            )
        )

        // When
        val flatMap = metadata.toFlatMap()

        // Then
        assertEquals("Test Title", flatMap[MetadataFields.TITLE])
        assertEquals("customValue", flatMap["metadata.customField"])
        assertEquals(0, flatMap["metadata.chunkIndex"])
    }

    @Test
    fun `toFlatMap should filter null values`() {
        // Given
        val metadata = DocumentMetadata(
            title = "Test Title",
            path = null,
            spaceKey = null
        )

        // When
        val flatMap = metadata.toFlatMap()

        // Then
        assertEquals(1, flatMap.size)
        assertEquals("Test Title", flatMap[MetadataFields.TITLE])
    }

    @Test
    fun `fromFlatMap should convert flat structure to DocumentMetadata`() {
        // Given
        val flatMap = mapOf(
            MetadataFields.TITLE to "Test Title",
            MetadataFields.PATH to "/test/path",
            MetadataFields.SPACE_KEY to "TEST",
            MetadataFields.KEYWORDS to "test, keywords",
            MetadataFields.ID to "123",
            MetadataFields.TYPE to "confluence-page"
        )

        // When
        val metadata = DocumentMetadata.fromFlatMap(flatMap)

        // Then
        assertEquals("Test Title", metadata.title)
        assertEquals("/test/path", metadata.path)
        assertEquals("TEST", metadata.spaceKey)
        assertEquals("test, keywords", metadata.keywords)
        assertEquals("123", metadata.id)
        assertEquals("confluence-page", metadata.type)
    }

    @Test
    fun `fromFlatMap should handle additional properties`() {
        // Given
        val flatMap = mapOf(
            MetadataFields.TITLE to "Test Title",
            "metadata.customField" to "customValue",
            "metadata.chunkIndex" to 0,
            "metadata.totalChunks" to 5
        )

        // When
        val metadata = DocumentMetadata.fromFlatMap(flatMap)

        // Then
        assertEquals("Test Title", metadata.title)
        assertEquals(3, metadata.additionalProperties.size)
        assertEquals("customValue", metadata.additionalProperties["customField"])
        assertEquals(0, metadata.additionalProperties["chunkIndex"])
        assertEquals(5, metadata.additionalProperties["totalChunks"])
    }

    @Test
    fun `fromFlatMap should ignore non-metadata fields`() {
        // Given
        val flatMap = mapOf(
            MetadataFields.TITLE to "Test Title",
            "id" to "doc123", // non-metadata field
            "content" to "some content", // non-metadata field
            "metadata.customField" to "customValue"
        )

        // When
        val metadata = DocumentMetadata.fromFlatMap(flatMap)

        // Then
        assertEquals("Test Title", metadata.title)
        assertEquals(1, metadata.additionalProperties.size)
        assertEquals("customValue", metadata.additionalProperties["customField"])
    }

    @Test
    fun `round trip conversion should preserve data - nested format`() {
        // Given
        val original = DocumentMetadata(
            title = "Test Title",
            path = "/test/path",
            spaceKey = "TEST",
            keywords = "test, keywords",
            id = "123",
            type = "confluence-page",
            additionalProperties = mapOf("custom" to "value")
        )

        // When
        val map = original.toMap()
        val converted = DocumentMetadata.fromMap(map)

        // Then
        assertEquals(original.title, converted.title)
        assertEquals(original.path, converted.path)
        assertEquals(original.spaceKey, converted.spaceKey)
        assertEquals(original.keywords, converted.keywords)
        assertEquals(original.id, converted.id)
        assertEquals(original.type, converted.type)
        assertEquals(original.additionalProperties, converted.additionalProperties)
    }

    @Test
    fun `round trip conversion should preserve data - flat format`() {
        // Given
        val original = DocumentMetadata(
            title = "Test Title",
            path = "/test/path",
            spaceKey = "TEST",
            keywords = "test, keywords",
            id = "123",
            type = "confluence-page",
            additionalProperties = mapOf("custom" to "value")
        )

        // When
        val flatMap = original.toFlatMap()
        val converted = DocumentMetadata.fromFlatMap(flatMap)

        // Then
        assertEquals(original.title, converted.title)
        assertEquals(original.path, converted.path)
        assertEquals(original.spaceKey, converted.spaceKey)
        assertEquals(original.keywords, converted.keywords)
        assertEquals(original.id, converted.id)
        assertEquals(original.type, converted.type)
        assertEquals(original.additionalProperties, converted.additionalProperties)
    }
}
