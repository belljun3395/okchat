package com.okestro.okchat.confluence.util

import com.okestro.okchat.confluence.model.ContentHierarchy
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.model.ContentType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentHierarchyVisualizerTest {

    private fun createSampleHierarchy(): ContentHierarchy {
        val grandchild1 = ContentNode(id = "4", title = "Grandchild 1", type = ContentType.PAGE)
        val grandchild2 = ContentNode(id = "5", title = "Grandchild 2", type = ContentType.PAGE)
        val child1 = ContentNode(
            id = "2",
            title = "Child Folder",
            type = ContentType.FOLDER,
            children = mutableListOf(grandchild1, grandchild2)
        )
        val child2 = ContentNode(id = "3", title = "Child Page", type = ContentType.PAGE)
        val root = ContentNode(
            id = "1",
            title = "Root Folder",
            type = ContentType.FOLDER,
            children = mutableListOf(child1, child2)
        )

        return ContentHierarchy(listOf(root))
    }

    @Test
    fun `visualize should include header and footer`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualize(hierarchy)

        // Then
        assertTrue(output.contains("Content Hierarchy"))
        assertTrue(output.contains("=".repeat(80)))
        assertTrue(output.contains("Summary:"))
    }

    @Test
    fun `visualize should show statistics`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualize(hierarchy)

        // Then
        assertTrue(output.contains("Total Nodes: 5"))
        assertTrue(output.contains("Folders: 2"))
        assertTrue(output.contains("Pages: 3"))
        assertTrue(output.contains("Max Depth: 3"))
    }

    @Test
    fun `visualize should show tree structure with icons`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualize(hierarchy)

        // Then
        assertTrue(output.contains("[Folder]"))
        assertTrue(output.contains("[Page]"))
        assertTrue(output.contains("Root Folder"))
        assertTrue(output.contains("Child Folder"))
        assertTrue(output.contains("Child Page"))
    }

    @Test
    fun `visualize should show tree connectors`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualize(hierarchy)

        // Then
        assertTrue(output.contains("├──") || output.contains("└──"))
    }

    @Test
    fun `visualize should show child counts`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualize(hierarchy)

        // Then
        // Root has 2 children, Child Folder has 2 children
        assertTrue(output.contains("(2)"))
    }

    @Test
    fun `visualize should show node IDs`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualize(hierarchy)

        // Then
        assertTrue(output.contains("[ID: 1]"))
        assertTrue(output.contains("[ID: 2]"))
        assertTrue(output.contains("[ID: 3]"))
    }

    @Test
    fun `visualizeCompact should show compact view`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualizeCompact(hierarchy)

        // Then
        assertTrue(output.contains("Content Hierarchy (Compact)"))
        assertTrue(output.contains("[F]")) // Folder icon
        assertTrue(output.contains("[P]")) // Page icon
        assertTrue(output.contains("Root Folder"))
    }

    @Test
    fun `visualizeCompact should use indentation for hierarchy`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val output = ContentHierarchyVisualizer.visualizeCompact(hierarchy)

        // Then - should have different indentation levels
        val lines = output.lines()
        val rootLine = lines.find { it.contains("Root Folder") }
        val childLine = lines.find { it.contains("Child Folder") }
        val grandchildLine = lines.find { it.contains("Grandchild") }

        assertTrue(rootLine != null)
        assertTrue(childLine != null)
        assertTrue(grandchildLine != null)

        // Child should be more indented than root
        val rootIndent = rootLine!!.takeWhile { it == ' ' }.length
        val childIndent = childLine!!.takeWhile { it == ' ' }.length
        val grandchildIndent = grandchildLine!!.takeWhile { it == ' ' }.length

        assertTrue(childIndent > rootIndent)
        assertTrue(grandchildIndent > childIndent)
    }

    @Test
    fun `HierarchyStats calculate should compute correct statistics`() {
        // Given
        val hierarchy = createSampleHierarchy()

        // When
        val stats = HierarchyStats.calculate(hierarchy)

        // Then
        assertEquals(5, stats.totalNodes)
        assertEquals(2, stats.folderCount)
        assertEquals(3, stats.pageCount)
        assertEquals(3, stats.maxDepth)
        // Root has 2 children, Child Folder has 2 children, others have 0
        // Average = (2 + 2 + 0 + 0 + 0) / 5 = 0.8
        assertEquals(0.8, stats.avgChildrenPerNode, 0.001)
    }

    @Test
    fun `HierarchyStats should handle empty hierarchy`() {
        // Given
        val emptyHierarchy = ContentHierarchy(emptyList())

        // When
        val stats = HierarchyStats.calculate(emptyHierarchy)

        // Then
        assertEquals(0, stats.totalNodes)
        assertEquals(0, stats.folderCount)
        assertEquals(0, stats.pageCount)
        assertEquals(0, stats.maxDepth)
        assertEquals(0.0, stats.avgChildrenPerNode)
    }

    @Test
    fun `HierarchyStats should handle single node`() {
        // Given
        val singleNode = ContentNode(id = "1", title = "Single", type = ContentType.PAGE)
        val hierarchy = ContentHierarchy(listOf(singleNode))

        // When
        val stats = HierarchyStats.calculate(hierarchy)

        // Then
        assertEquals(1, stats.totalNodes)
        assertEquals(0, stats.folderCount)
        assertEquals(1, stats.pageCount)
        assertEquals(1, stats.maxDepth)
        assertEquals(0.0, stats.avgChildrenPerNode)
    }

    @Test
    fun `visualize should handle empty hierarchy`() {
        // Given
        val emptyHierarchy = ContentHierarchy(emptyList())

        // When
        val output = ContentHierarchyVisualizer.visualize(emptyHierarchy)

        // Then
        assertTrue(output.contains("Total Nodes: 0"))
        assertTrue(output.contains("Folders: 0"))
        assertTrue(output.contains("Pages: 0"))
    }
}
