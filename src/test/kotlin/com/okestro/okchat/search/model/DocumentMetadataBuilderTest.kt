package com.okestro.okchat.search.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentMetadataBuilderTest {

    @Test
    fun `metadata DSL should create DocumentMetadata with all properties`() {
        // When
        val metadata = metadata {
            id = "123"
            title = "Test Page"
            type = "confluence-page"
            spaceKey = "TEST"
            path = "Space > Folder > Page"
            keywords = listOf("keyword1", "keyword2", "keyword3")
        }

        // Then
        assertEquals("123", metadata.id)
        assertEquals("Test Page", metadata.title)
        assertEquals("confluence-page", metadata.type)
        assertEquals("TEST", metadata.spaceKey)
        assertEquals("Space > Folder > Page", metadata.path)
        assertEquals("keyword1, keyword2, keyword3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should handle minimal properties`() {
        // When
        val metadata = metadata {
            title = "Minimal Page"
        }

        // Then
        assertEquals("Minimal Page", metadata.title)
        assertNull(metadata.id)
        assertNull(metadata.path)
        assertNull(metadata.spaceKey)
        assertNull(metadata.type)
        assertNull(metadata.keywords)
    }

    @Test
    fun `metadata DSL should support keywords from list`() {
        // When
        val metadata = metadata {
            keywords = listOf("keyword1", "keyword2", "keyword3")
        }

        // Then
        assertEquals("keyword1, keyword2, keyword3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should support keywords from comma-separated string`() {
        // When
        val metadata = metadata {
            keywords("keyword1, keyword2, keyword3")
        }

        // Then
        assertEquals("keyword1, keyword2, keyword3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should support adding single keyword`() {
        // When
        val metadata = metadata {
            keyword("keyword1")
            keyword("keyword2")
            keyword("keyword3")
        }

        // Then
        assertEquals("keyword1, keyword2, keyword3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should support adding multiple keywords with vararg`() {
        // When
        val metadata = metadata {
            keywords("keyword1", "keyword2", "keyword3")
        }

        // Then
        assertEquals("keyword1, keyword2, keyword3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should support additional properties with infix notation`() {
        // When
        val metadata = metadata {
            title = "Test Page"
            "isEmpty" to false
            "chunkIndex" to 0
            "totalChunks" to 5
        }

        // Then
        assertEquals("Test Page", metadata.title)
        assertEquals(3, metadata.additionalProperties.size)
        assertEquals(false, metadata.additionalProperties["isEmpty"])
        assertEquals(0, metadata.additionalProperties["chunkIndex"])
        assertEquals(5, metadata.additionalProperties["totalChunks"])
    }

    @Test
    fun `metadata DSL should support additional properties with property method`() {
        // When
        val metadata = metadata {
            title = "Test Page"
            property("customField", "customValue")
            property("numericField", 42)
        }

        // Then
        assertEquals("Test Page", metadata.title)
        assertEquals(2, metadata.additionalProperties.size)
        assertEquals("customValue", metadata.additionalProperties["customField"])
        assertEquals(42, metadata.additionalProperties["numericField"])
    }

    @Test
    fun `metadata DSL should handle empty keywords list`() {
        // When
        val metadata = metadata {
            title = "Test Page"
            keywords = emptyList()
        }

        // Then
        assertEquals("Test Page", metadata.title)
        assertNull(metadata.keywords)
    }

    @Test
    fun `metadata DSL should trim and filter blank keywords from string`() {
        // When
        val metadata = metadata {
            keywords("  keyword1  ,  , keyword2  , keyword3  ")
        }

        // Then
        assertEquals("keyword1, keyword2, keyword3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should allow keyword list overwrite`() {
        // When
        val metadata = metadata {
            keywords = listOf("old1", "old2")
            keywords = listOf("new1", "new2", "new3")
        }

        // Then
        assertEquals("new1, new2, new3", metadata.keywords)
    }

    @Test
    fun `metadata DSL should combine keywords from different methods`() {
        // When
        val metadata = metadata {
            keyword("keyword1")
            keywords("keyword2", "keyword3")
            keyword("keyword4")
        }

        // Then
        assertEquals("keyword1, keyword2, keyword3, keyword4", metadata.keywords)
    }

    @Test
    fun `toFlatMap should work with DSL created metadata`() {
        // Given
        val metadata = metadata {
            id = "123"
            title = "Test Page"
            type = "confluence-page"
            spaceKey = "TEST"
            path = "Space > Page"
            keywords = listOf("key1", "key2")
            "isEmpty" to false
        }

        // When
        val flatMap = metadata.toFlatMap()

        // Then
        assertEquals("123", flatMap[MetadataFields.ID])
        assertEquals("Test Page", flatMap[MetadataFields.TITLE])
        assertEquals("confluence-page", flatMap[MetadataFields.TYPE])
        assertEquals("TEST", flatMap[MetadataFields.SPACE_KEY])
        assertEquals("Space > Page", flatMap[MetadataFields.PATH])
        assertEquals("key1, key2", flatMap[MetadataFields.KEYWORDS])
        assertEquals(false, flatMap["metadata.isEmpty"])
    }

    @Test
    fun `DSL should support complex real-world scenario`() {
        // Simulating ConfluenceSyncTask usage
        val nodeId = "page123"
        val pageTitle = "Project Documentation"
        val currentSpaceKey = "PROJ"
        val currentPath = "Projects > Documentation > API"
        val allKeywords = listOf("project", "documentation", "api", "rest")
        val isEmpty = false

        // When
        val metadata = metadata {
            this.id = nodeId
            this.title = pageTitle
            this.type = "confluence-page"
            this.spaceKey = currentSpaceKey
            this.path = currentPath
            this.keywords = allKeywords
            "isEmpty" to isEmpty
        }

        // Then
        assertEquals("page123", metadata.id)
        assertEquals("Project Documentation", metadata.title)
        assertEquals("confluence-page", metadata.type)
        assertEquals("PROJ", metadata.spaceKey)
        assertEquals("Projects > Documentation > API", metadata.path)
        assertEquals("project, documentation, api, rest", metadata.keywords)
        assertEquals(false, metadata.additionalProperties["isEmpty"])

        // Verify flat map conversion
        val flatMap = metadata.toFlatMap()
        assertNotNull(flatMap[MetadataFields.ID])
        assertNotNull(flatMap[MetadataFields.TITLE])
        assertNotNull(flatMap["metadata.isEmpty"])
    }

    @Test
    fun `DSL should support chunked document metadata creation`() {
        // Simulating chunk metadata creation
        val nodeId = "page123"
        val allKeywords = listOf("keyword1", "keyword2")
        val chunkIndex = 2
        val totalChunks = 5

        // When
        val metadata = metadata {
            this.id = nodeId
            this.keywords = allKeywords
            "chunkIndex" to chunkIndex
            "totalChunks" to totalChunks
        }

        // Then
        assertEquals("page123", metadata.id)
        assertEquals("keyword1, keyword2", metadata.keywords)
        assertEquals(2, metadata.additionalProperties["chunkIndex"])
        assertEquals(5, metadata.additionalProperties["totalChunks"])
    }

    @Test
    fun `DSL builder should be reusable`() {
        // When
        val metadata1 = metadata {
            title = "Page 1"
            keywords = listOf("key1")
        }

        val metadata2 = metadata {
            title = "Page 2"
            keywords = listOf("key2", "key3")
        }

        // Then
        assertEquals("Page 1", metadata1.title)
        assertEquals("key1", metadata1.keywords)
        assertEquals("Page 2", metadata2.title)
        assertEquals("key2, key3", metadata2.keywords)
    }

    @Test
    fun `DSL should handle null values correctly`() {
        // When
        val metadata = metadata {
            title = "Test Page"
            id = null
            path = null
        }

        // Then
        assertEquals("Test Page", metadata.title)
        assertNull(metadata.id)
        assertNull(metadata.path)
    }

    @Test
    fun `DSL should support dynamic property values`() {
        // Given
        val dynamicValue = calculateSomeValue()
        val dynamicBoolean = checkSomeCondition()

        // When
        val metadata = metadata {
            title = "Test Page"
            "computedValue" to dynamicValue
            "conditionResult" to dynamicBoolean
        }

        // Then
        assertEquals(42, metadata.additionalProperties["computedValue"])
        assertEquals(true, metadata.additionalProperties["conditionResult"])
    }

    // Helper methods for testing
    private fun calculateSomeValue(): Int = 42
    private fun checkSomeCondition(): Boolean = true
}
