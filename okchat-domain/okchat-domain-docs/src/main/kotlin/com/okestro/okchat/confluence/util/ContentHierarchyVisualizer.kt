package com.okestro.okchat.confluence.util

import com.okestro.okchat.confluence.model.ContentHierarchy
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.model.ContentType

/**
 * Utility for visualizing Confluence content hierarchy as ASCII tree
 */
object ContentHierarchyVisualizer {

    /**
     * Visualize the entire hierarchy as a tree
     */
    fun visualize(hierarchy: ContentHierarchy): String {
        return buildString {
            appendLine("Content Hierarchy")
            appendLine("=".repeat(80))
            appendLine()

            val stats = HierarchyStats.calculate(hierarchy)
            appendStats(stats)
            appendLine()

            hierarchy.rootNodes.forEachIndexed { index, node ->
                val isLast = index == hierarchy.rootNodes.size - 1
                visualizeNode(node, "", isLast)
            }

            appendLine()
            appendLine("=".repeat(80))
            appendSummary(stats)
        }
    }

    /**
     * Generate compact view (only titles and types)
     */
    fun visualizeCompact(hierarchy: ContentHierarchy): String {
        return buildString {
            appendLine("Content Hierarchy (Compact)")
            appendLine("-".repeat(60))
            hierarchy.rootNodes.forEach { node ->
                visualizeNodeCompact(node, 0)
            }
        }
    }

    // Private helper methods
    private fun StringBuilder.visualizeNode(
        node: ContentNode,
        prefix: String,
        isLast: Boolean
    ) {
        val icon = if (node.type == ContentType.FOLDER) "[Folder]" else "[Page]"
        val connector = if (isLast) "└── " else "├── "
        val idInfo = " [ID: ${node.id}]"
        val childInfo = if (node.children.isNotEmpty()) " (${node.children.size})" else ""

        appendLine("$prefix$connector$icon ${node.title}$childInfo$idInfo")

        val childPrefix = prefix + if (isLast) "    " else "│   "
        node.children.forEachIndexed { index, child ->
            val isChildLast = index == node.children.size - 1
            visualizeNode(child, childPrefix, isChildLast)
        }
    }

    private fun StringBuilder.visualizeNodeMarkdown(node: ContentNode, depth: Int) {
        val indent = "  ".repeat(depth)
        val icon = if (node.type == ContentType.FOLDER) "[Folder]" else "[Page]"
        val childInfo = if (node.children.isNotEmpty()) " *(${node.children.size} items)*" else ""

        appendLine("$indent- $icon **${node.title}**$childInfo")

        node.children.forEach { child ->
            visualizeNodeMarkdown(child, depth + 1)
        }
    }

    private fun StringBuilder.visualizeNodeCompact(node: ContentNode, depth: Int) {
        val indent = "  ".repeat(depth)
        val icon = if (node.type == ContentType.FOLDER) "[F]" else "[P]"

        appendLine("$indent$icon ${node.title}")

        node.children.forEach { child ->
            visualizeNodeCompact(child, depth + 1)
        }
    }

    private fun StringBuilder.appendStats(stats: HierarchyStats) {
        appendLine("Statistics:")
        appendLine("  - Total Nodes: ${stats.totalNodes}")
        appendLine("  - Folders: ${stats.folderCount}")
        appendLine("  - Pages: ${stats.pageCount}")
        appendLine("  - Max Depth: ${stats.maxDepth}")
        appendLine("  - Avg Children/Node: ${"%.1f".format(stats.avgChildrenPerNode)}")
    }

    private fun StringBuilder.appendSummary(stats: HierarchyStats) {
        appendLine("Summary: ${stats.folderCount} folders, ${stats.pageCount} pages, max depth ${stats.maxDepth}")
    }

    private fun filterNode(node: ContentNode, filter: (ContentNode) -> Boolean): ContentNode? {
        if (!filter(node) && node.children.isEmpty()) {
            return null
        }

        val filteredChildren = node.children
            .mapNotNull { filterNode(it, filter) }
            .toMutableList()

        return if (filter(node) || filteredChildren.isNotEmpty()) {
            node.copy(children = filteredChildren)
        } else {
            null
        }
    }
}

/**
 * Statistics about the content hierarchy
 */
data class HierarchyStats(
    val totalNodes: Int,
    val folderCount: Int,
    val pageCount: Int,
    val maxDepth: Int,
    val avgChildrenPerNode: Double
) {
    companion object {
        fun calculate(hierarchy: ContentHierarchy): HierarchyStats {
            val allNodes = hierarchy.getAllNodes()
            val folders = allNodes.filter { it.type == ContentType.FOLDER }
            val pages = allNodes.filter { it.type == ContentType.PAGE }
            val maxDepth = calculateMaxDepth(hierarchy)
            val avgChildren = if (allNodes.isNotEmpty()) {
                allNodes.sumOf { it.children.size }.toDouble() / allNodes.size
            } else {
                0.0
            }

            return HierarchyStats(
                totalNodes = allNodes.size,
                folderCount = folders.size,
                pageCount = pages.size,
                maxDepth = maxDepth,
                avgChildrenPerNode = avgChildren
            )
        }

        private fun calculateMaxDepth(hierarchy: ContentHierarchy): Int {
            fun nodeDepth(node: ContentNode, currentDepth: Int): Int {
                return if (node.children.isEmpty()) {
                    currentDepth
                } else {
                    node.children.maxOf { nodeDepth(it, currentDepth + 1) }
                }
            }

            return if (hierarchy.rootNodes.isEmpty()) {
                0
            } else {
                hierarchy.rootNodes.maxOf { nodeDepth(it, 1) }
            }
        }
    }
}
