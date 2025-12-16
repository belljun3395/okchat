package com.okestro.okchat.search.index

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocumentIndexTest {

    @Test
    fun `getMapping should return correct index structure`() {
        val mapping = DocumentIndex.getMapping()

        // Root properties
        assertTrue(mapping.containsKey("properties"))
        val properties = mapping["properties"] as Map<*, *>

        // Assert field existence and types
        assertFieldType(properties, DocumentIndex.Fields.ID, "keyword")
        assertFieldType(properties, DocumentIndex.Fields.CONTENT, "text")

        // Assert embedding configuration
        val embedding = properties[DocumentIndex.Fields.EMBEDDING] as Map<*, *>
        assertEquals("knn_vector", embedding["type"])
        assertEquals(1536, embedding["dimension"])

        // Assert metadata object exists
        assertTrue(properties.containsKey("metadata"))
        val metadata = properties["metadata"] as Map<*, *>
        assertTrue(metadata.containsKey("properties"))
        val metadataProperties = metadata["properties"] as Map<*, *>

        // Assert metadata fields (extracting leaf name from dot notation)
        assertFieldType(metadataProperties, "title", "text")
        assertFieldType(metadataProperties, "path", "keyword")
        assertFieldType(metadataProperties, "spaceKey", "keyword")
        assertFieldType(metadataProperties, "keywords", "text")
        assertFieldType(metadataProperties, "id", "keyword")
        assertFieldType(metadataProperties, "type", "keyword")
        assertFieldType(metadataProperties, "knowledgeBaseId", "long")
        assertFieldType(metadataProperties, "webUrl", "keyword")
        assertFieldType(metadataProperties, "isEmpty", "boolean")

        // PDF specific fields
        assertFieldType(metadataProperties, "pageId", "keyword")
        assertFieldType(metadataProperties, "attachmentTitle", "text")
        assertFieldType(metadataProperties, "totalPdfPages", "integer")
        assertFieldType(metadataProperties, "fileSize", "long")
    }

    private fun assertFieldType(properties: Map<*, *>, fieldName: String, expectedType: String) {
        assertTrue(properties.containsKey(fieldName), "Field $fieldName should exist")
        val field = properties[fieldName] as Map<*, *>
        assertEquals(expectedType, field["type"], "Field $fieldName should have type $expectedType")
    }
}
