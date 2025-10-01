package com.okestro.okchat.confluence.service

import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.client.Page
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class ConfluenceService(
    private val confluenceClient: ConfluenceClient
) {
    private val log = KotlinLogging.logger {}

    /**
     * Get space ID by space key
     */
    fun getSpaceIdByKey(spaceKey: String): String {
        log.info { "Fetching space ID for key: $spaceKey" }
        val response = confluenceClient.getSpaceByKey(spaceKey)
        val space =
            response.results.firstOrNull() ?: throw IllegalArgumentException("Space not found with key: $spaceKey")
        log.info { "Found space: ${space.name} (ID: ${space.id})" }
        return space.id
    }

    /**
     * Get all content in a space with hierarchical structure
     */
    fun getSpaceContentHierarchy(spaceId: String): ContentHierarchy {
        log.info { "Fetching content for space ID: $spaceId" }

        // Fetch all pages (includes folders)
        val allContent = getAllPages(spaceId)

        // Identify folders: items whose IDs appear as parentId with parentType="folder"
        val folderIds = allContent.filter { it.parentType == "folder" }.mapNotNull { it.parentId }.toSet()

        // Find missing parent IDs (these are top-level folders not returned by the API)
        val allItemIds = allContent.map { it.id }.toSet()
        val allParentIds = allContent.mapNotNull { it.parentId }.toSet()
        val missingFolderIds = allParentIds - allItemIds

        log.info { "Total content: ${allContent.size}, Identified folder IDs: ${folderIds.size}, Missing top-level folders: ${missingFolderIds.size}" }

        // Fetch actual folder information for missing folders
        val fetchedFolders = missingFolderIds.mapNotNull { folderId ->
            try {
                val folderResponse = confluenceClient.getFolderById(folderId)
                Page(
                    id = folderResponse.id,
                    title = folderResponse.title,
                    parentId = folderResponse.parentId,
                    parentType = folderResponse.parentType,
                    spaceId = spaceId,
                    status = folderResponse.status,
                    version = folderResponse.version
                )
            } catch (e: Exception) {
                log.warn { "Failed to fetch folder $folderId: ${e.message}" }
                null
            }
        }

        log.info { "Successfully fetched ${fetchedFolders.size} folder details" }

        val allContentWithFolders = allContent + fetchedFolders
        val allFolderIds = folderIds + missingFolderIds

        // Build hierarchical structure
        return buildHierarchy(allContentWithFolders, allFolderIds)
    }

    /**
     * Get all pages in a space (with pagination)
     */
    fun getAllPages(spaceId: String): List<Page> {
        val allPages = mutableListOf<Page>()
        var cursor: String? = null

        do {
            val response = confluenceClient.getPagesBySpaceId(spaceId, cursor)
            allPages.addAll(response.results)
            cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            log.debug { "Fetched ${response.results.size} pages, cursor: $cursor" }
        } while (cursor != null)

        return allPages
    }

    /**
     * Build hierarchical structure from all content (pages and folders)
     */
    private fun buildHierarchy(allContent: List<Page>, folderIds: Set<String>): ContentHierarchy {
        // Create node map for quick lookup
        val nodeMap = mutableMapOf<String, ContentNode>()

        // Add all content to node map
        allContent.forEach { item ->
            val contentType = if (item.id in folderIds) ContentType.FOLDER else ContentType.PAGE
            nodeMap[item.id] = ContentNode(
                id = item.id,
                title = item.title,
                type = contentType,
                parentId = item.parentId,
                body = item.body?.storage?.value
            )
        }

        // Build tree structure
        val rootNodes = mutableListOf<ContentNode>()
        nodeMap.values.forEach { node ->
            if (node.parentId == null) {
                // Root level content
                rootNodes.add(node)
            } else {
                // Child content - add to parent
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }

        return ContentHierarchy(rootNodes)
    }
}

// Data classes for hierarchical structure
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
     * Get all folders
     */
    fun getAllFolders(): List<ContentNode> {
        return getAllNodes().filter { it.type == ContentType.FOLDER }
    }

    /**
     * Get all pages
     */
    fun getAllPages(): List<ContentNode> {
        return getAllNodes().filter { it.type == ContentType.PAGE }
    }

    /**
     * Find node by ID
     */
    fun findNodeById(id: String): ContentNode? {
        return getAllNodes().find { it.id == id }
    }

    /**
     * Find nodes by title (case-insensitive partial match)
     */
    fun findNodesByTitle(title: String): List<ContentNode> {
        return getAllNodes().filter { it.title.contains(title, ignoreCase = true) }
    }

    /**
     * Get total count of all nodes
     */
    fun getTotalCount(): Int {
        return getAllNodes().size
    }

    /**
     * Get count by type
     */
    fun getCountByType(type: ContentType): Int {
        return getAllNodes().count { it.type == type }
    }

    /**
     * Get max depth of the hierarchy
     */
    fun getMaxDepth(): Int {
        fun calculateDepth(node: ContentNode): Int {
            return if (node.children.isEmpty()) {
                1
            } else {
                1 + (node.children.maxOfOrNull { calculateDepth(it) } ?: 0)
            }
        }
        return rootNodes.maxOfOrNull { calculateDepth(it) } ?: 0
    }

    /**
     * Get path to a node (from root to node)
     */
    fun getPathToNode(targetId: String): List<ContentNode>? {
        fun findPath(node: ContentNode, path: MutableList<ContentNode>): Boolean {
            path.add(node)
            if (node.id == targetId) {
                return true
            }
            for (child in node.children) {
                if (findPath(child, path)) {
                    return true
                }
            }
            path.removeAt(path.size - 1)
            return false
        }

        for (root in rootNodes) {
            val path = mutableListOf<ContentNode>()
            if (findPath(root, path)) {
                return path
            }
        }
        return null
    }
}

data class ContentNode(
    val id: String,
    val title: String,
    val type: ContentType,
    val parentId: String? = null,
    val children: MutableList<ContentNode> = mutableListOf(),
    val body: String? = null
)

enum class ContentType {
    FOLDER, PAGE
}
