package com.okestro.okchat.confluence.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentNodeTest {

    @Test
    fun `hasChildren should return true when node has children`() {
        // Given
        val parent = ContentNode(
            id = "1",
            title = "Parent",
            type = ContentType.FOLDER,
            children = mutableListOf(
                ContentNode(id = "2", title = "Child", type = ContentType.PAGE)
            )
        )
        val leaf = ContentNode(id = "3", title = "Leaf", type = ContentType.PAGE)

        // Then
        assertTrue(parent.hasChildren())
        assertFalse(leaf.hasChildren())
    }

    @Test
    fun `getChildCount should return correct count`() {
        // Given
        val parent = ContentNode(
            id = "1",
            title = "Parent",
            type = ContentType.FOLDER,
            children = mutableListOf(
                ContentNode(id = "2", title = "Child1", type = ContentType.PAGE),
                ContentNode(id = "3", title = "Child2", type = ContentType.PAGE)
            )
        )

        // Then
        assertEquals(2, parent.getChildCount())
    }

    @Test
    fun `getDescendantCount should count all descendants recursively`() {
        // Given
        val grandchild1 = ContentNode(id = "4", title = "Grandchild1", type = ContentType.PAGE)
        val grandchild2 = ContentNode(id = "5", title = "Grandchild2", type = ContentType.PAGE)
        val child1 = ContentNode(
            id = "2",
            title = "Child1",
            type = ContentType.FOLDER,
            children = mutableListOf(grandchild1, grandchild2)
        )
        val child2 = ContentNode(id = "3", title = "Child2", type = ContentType.PAGE)
        val root = ContentNode(
            id = "1",
            title = "Root",
            type = ContentType.FOLDER,
            children = mutableListOf(child1, child2)
        )

        // Then
        // child1 (2 grandchildren) + child2 = 4 total descendants
        assertEquals(4, root.getDescendantCount())
        // grandchild1 + grandchild2 = 2 descendants
        assertEquals(2, child1.getDescendantCount())
        // leaf node has 0 descendants
        assertEquals(0, child2.getDescendantCount())
    }

    @Test
    fun `toString should include emoji and child count`() {
        // Given
        val folder = ContentNode(
            id = "1",
            title = "Test Folder",
            type = ContentType.FOLDER,
            children = mutableListOf(
                ContentNode(id = "2", title = "Child", type = ContentType.PAGE)
            )
        )
        val page = ContentNode(
            id = "3",
            title = "Test Page",
            type = ContentType.PAGE
        )

        // Then
        assertTrue(folder.toString().contains("📁"))
        assertTrue(folder.toString().contains("Test Folder"))
        assertTrue(folder.toString().contains("(1)")) // child count
        assertTrue(folder.toString().contains("[ID: 1]"))

        assertTrue(page.toString().contains("📄"))
        assertTrue(page.toString().contains("Test Page"))
        assertFalse(page.toString().contains("(")) // no child count
    }

    @Test
    fun `parentId should be nullable`() {
        // Given
        val rootNode = ContentNode(
            id = "1",
            title = "Root",
            type = ContentType.FOLDER,
            parentId = null
        )
        val childNode = ContentNode(
            id = "2",
            title = "Child",
            type = ContentType.PAGE,
            parentId = "1"
        )

        // Then
        assertEquals(null, rootNode.parentId)
        assertEquals("1", childNode.parentId)
    }
}
