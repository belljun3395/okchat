package com.okestro.okchat.confluence.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentHierarchyTest {

    private fun createSampleHierarchy(): ContentHierarchy {
        val grandchild1 = ContentNode(id = "4", title = "Grandchild 1", type = ContentType.PAGE)
        val grandchild2 = ContentNode(id = "5", title = "Grandchild 2", type = ContentType.PAGE)
        val child1 = ContentNode(
            id = "2",
            title = "Child 1",
            type = ContentType.FOLDER,
            children = mutableListOf(grandchild1, grandchild2)
        )
        val child2 = ContentNode(id = "3", title = "Child 2", type = ContentType.PAGE)
        val root = ContentNode(
            id = "1",
            title = "Root",
            type = ContentType.FOLDER,
            children = mutableListOf(child1, child2)
        )

        return ContentHierarchy(listOf(root))
    }

    @Test
    fun `getAllNodes should return all nodes in hierarchy`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val allNodes = hierarchy.getAllNodes()

        // Then
        assertEquals(5, allNodes.size) // root + 2 children + 2 grandchildren
    }

    @Test
    fun `getAllFolders should return only folder nodes`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val folders = hierarchy.getAllFolders()

        // Then
        assertEquals(2, folders.size) // Root and Child 1
        assertTrue(folders.all { it.type == ContentType.FOLDER })
    }

    @Test
    fun `getAllPages should return only page nodes`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val pages = hierarchy.getAllPages()

        // Then
        assertEquals(3, pages.size) // Child 2, Grandchild 1, Grandchild 2
        assertTrue(pages.all { it.type == ContentType.PAGE })
    }

    @Test
    fun `findNodeById should find existing node`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val node = hierarchy.findNodeById("4")

        // Then
        assertNotNull(node)
        assertEquals("Grandchild 1", node.title)
    }

    @Test
    fun `findNodeById should return null for non-existent node`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val node = hierarchy.findNodeById("999")

        // Then
        assertNull(node)
    }

    @Test
    fun `findNodesByTitle should find nodes with matching title`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val nodes = hierarchy.findNodesByTitle("child")

        // Then
        assertEquals(4, nodes.size) // "Child 1", "Child 2", "Grandchild 1", "Grandchild 2"
        assertTrue(nodes.all { it.title.contains("Child", ignoreCase = true) })
    }

    @Test
    fun `getTotalCount should return total number of nodes`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val count = hierarchy.getTotalCount()

        // Then
        assertEquals(5, count)
    }

    @Test
    fun `getCountByType should count nodes by type`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val folderCount = hierarchy.getCountByType(ContentType.FOLDER)
        val pageCount = hierarchy.getCountByType(ContentType.PAGE)

        // Then
        assertEquals(2, folderCount)
        assertEquals(3, pageCount)
    }

    @Test
    fun `getMaxDepth should calculate maximum depth correctly`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val maxDepth = hierarchy.getMaxDepth()

        // Then
        assertEquals(3, maxDepth) // Root -> Child 1 -> Grandchild
    }

    @Test
    fun `getMaxDepth should return 0 for empty hierarchy`() {
        // Given
        val emptyHierarchy = ContentHierarchy(emptyList())

        // When
        val maxDepth = emptyHierarchy.getMaxDepth()

        // Then
        assertEquals(0, maxDepth)
    }

    @Test
    fun `getPathToNode should return path from root to target node`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val path = hierarchy.getPathToNode("4") // Grandchild 1

        // Then
        assertNotNull(path)
        assertEquals(3, path.size)
        assertEquals("1", path[0].id) // Root
        assertEquals("2", path[1].id) // Child 1
        assertEquals("4", path[2].id) // Grandchild 1
    }

    @Test
    fun `getPathToNode should return null for non-existent node`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val path = hierarchy.getPathToNode("999")

        // Then
        assertNull(path)
    }

    @Test
    fun `getStatistics should calculate correct statistics`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val stats = hierarchy.getStatistics()

        // Then
        assertEquals(5, stats.totalNodes)
        assertEquals(2, stats.folders)
        assertEquals(3, stats.pages)
        assertEquals(3, stats.maxDepth)
        // Root has 2 children, Child 1 has 2 children, others have 0
        // Average = (2 + 2 + 0 + 0 + 0) / 5 = 0.8
        assertEquals(0.8, stats.avgChildrenPerNode, 0.001)
    }

    @Test
    fun `empty hierarchy should have zero statistics`() {
        // Given
        val emptyHierarchy = ContentHierarchy(emptyList())

        // When
        val stats = emptyHierarchy.getStatistics()

        // Then
        assertEquals(0, stats.totalNodes)
        assertEquals(0, stats.folders)
        assertEquals(0, stats.pages)
        assertEquals(0, stats.maxDepth)
        assertEquals(0.0, stats.avgChildrenPerNode)
    }
}
