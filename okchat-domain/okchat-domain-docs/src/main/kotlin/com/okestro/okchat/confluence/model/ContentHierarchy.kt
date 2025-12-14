package com.okestro.okchat.confluence.model

data class ContentHierarchy(
    val rootNodes: List<ContentNode>
) {
    /**
     * Get all nodes as a flat list
     */
    fun getAllNodes(): List<ContentNode> {
        val allNodes = mutableListOf<ContentNode>()
        fun collectNodes(node: ContentNode) {
            allNodes.add(node)
            node.children.forEach { collectNodes(it) }
        }
        rootNodes.forEach { collectNodes(it) }
        return allNodes
    }

    /**
     * Get all folder nodes
     */
    fun getAllFolders(): List<ContentNode> {
        return getAllNodes().filter { it.type == ContentType.FOLDER }
    }

    /**
     * Get all page nodes
     */
    fun getAllPages(): List<ContentNode> {
        return getAllNodes().filter { it.type == ContentType.PAGE }
    }

    /**
     * Find a node by ID
     */
    fun findNodeById(id: String): ContentNode? {
        return getAllNodes().find { it.id == id }
    }

    /**
     * Find nodes by title
     */
    fun findNodesByTitle(title: String): List<ContentNode> {
        return getAllNodes().filter { it.title.contains(title, ignoreCase = true) }
    }

    /**
     * Get total count of all nodes
     */
    fun getTotalCount(): Int = getAllNodes().size

    /**
     * Get count by type
     */
    fun getCountByType(type: ContentType): Int {
        return getAllNodes().count { it.type == type }
    }

    /**
     * Get maximum depth of the hierarchy
     */
    fun getMaxDepth(): Int {
        fun calculateDepth(node: ContentNode): Int {
            if (node.children.isEmpty()) return 1
            return 1 + (node.children.maxOfOrNull { calculateDepth(it) } ?: 0)
        }

        if (rootNodes.isEmpty()) return 0
        return rootNodes.maxOf { calculateDepth(it) }
    }

    /**
     * Get path from root to a specific node
     */
    fun getPathToNode(targetId: String): List<ContentNode>? {
        fun findPath(node: ContentNode, path: MutableList<ContentNode>): Boolean {
            path.add(node)
            if (node.id == targetId) return true

            for (child in node.children) {
                if (findPath(child, path)) return true
            }

            path.removeAt(path.size - 1)
            return false
        }

        for (root in rootNodes) {
            val path = mutableListOf<ContentNode>()
            if (findPath(root, path)) return path
        }
        return null
    }

    /**
     * Get statistics
     */
    fun getStatistics(): HierarchyStatistics {
        val allNodes = getAllNodes()
        val folders = allNodes.count { it.type == ContentType.FOLDER }
        val pages = allNodes.count { it.type == ContentType.PAGE }
        val maxDepth = getMaxDepth()
        val avgChildren = if (allNodes.isNotEmpty()) {
            allNodes.sumOf { it.children.size }.toDouble() / allNodes.size
        } else {
            0.0
        }

        return HierarchyStatistics(
            totalNodes = allNodes.size,
            folders = folders,
            pages = pages,
            maxDepth = maxDepth,
            avgChildrenPerNode = avgChildren
        )
    }
}

data class HierarchyStatistics(
    val totalNodes: Int,
    val folders: Int,
    val pages: Int,
    val maxDepth: Int,
    val avgChildrenPerNode: Double
)
