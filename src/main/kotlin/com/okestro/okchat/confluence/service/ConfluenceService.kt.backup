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

        // Fetch all items (pages and folders) recursively
        val allContent = getAllPages(spaceId)

        log.info { "Total items fetched: ${allContent.size}" }

        // Identify folders using parentType field (children report their parent's type)
        val folderIdsFromChildren = allContent
            .filter { it.parentType?.equals("folder", ignoreCase = true) == true }
            .mapNotNull { it.parentId }
            .toSet()

        // Also include explicitly marked folders
        val explicitFolderIds = allContent
            .filter { it.type?.equals("folder", ignoreCase = true) == true }
            .map { it.id }
            .toSet()

        val knownFolderIds = (folderIdsFromChildren + explicitFolderIds).toMutableSet()

        log.info {
            "Folder identification: ${explicitFolderIds.size} explicit + " +
                "${folderIdsFromChildren.size} from parentType = ${knownFolderIds.size} total"
        }

        // Find missing parent IDs (these could be folders or pages not in the initial fetch)
        val allItemIds = allContent.map { it.id }.toSet()
        val allParentIds = allContent.mapNotNull { it.parentId }.toSet()
        var missingParentIds = allParentIds - allItemIds

        log.info { "Initial missing parents: ${missingParentIds.size}" }

        // Fetch missing parents iteratively until no more missing (up to 10 levels for deeply nested structures)
        val fetchedAncestors = mutableListOf<Page>()
        var safety = 0
        while (missingParentIds.isNotEmpty() && safety++ < 10) {
            log.info { "Fetching ${missingParentIds.size} missing parent(s) (iteration $safety)" }

            val newlyFetched = missingParentIds.mapNotNull { parentId ->
                // Try folder first
                try {
                    val folderResponse = confluenceClient.getFolderById(parentId)
                    knownFolderIds.add(folderResponse.id)
                    log.info { "  └─ Fetched folder: ${folderResponse.title} (ID: $parentId)" }
                    Page(
                        id = folderResponse.id,
                        title = folderResponse.title,
                        parentId = folderResponse.parentId,
                        parentType = folderResponse.parentType,
                        spaceId = spaceId,
                        status = folderResponse.status,
                        version = folderResponse.version,
                        type = "folder"
                    )
                } catch (_: Exception) {
                    // Try page as fallback
                    try {
                        val pageResponse = confluenceClient.getPageById(parentId)
                        log.info { "  └─ Fetched page: ${pageResponse.title} (ID: $parentId)" }
                        pageResponse
                    } catch (e: Exception) {
                        log.warn { "  └─ Failed to fetch parent $parentId: ${e.message}" }
                        null
                    }
                }
            }

            if (newlyFetched.isEmpty()) {
                log.warn { "Could not fetch any of the ${missingParentIds.size} missing parents - they may not exist or be inaccessible" }
                break
            }

            fetchedAncestors.addAll(newlyFetched)

            // Recompute missing parents including newly fetched ones
            val current = allContent + fetchedAncestors
            val currentIds = current.map { it.id }.toSet()
            val currentParentIds = current.mapNotNull { it.parentId }.toSet()
            val newMissingParentIds = currentParentIds - currentIds

            if (newMissingParentIds == missingParentIds) {
                log.warn { "No progress in fetching parents - stopping iteration" }
                break
            }

            missingParentIds = newMissingParentIds
        }

        log.info { "Fetched ${fetchedAncestors.size} missing ancestor(s) total across $safety iteration(s)" }

        val allContentWithAncestors = allContent + fetchedAncestors

        // Final check for still-missing parents
        val finalItemIds = allContentWithAncestors.map { it.id }.toSet()
        val finalParentIds = allContentWithAncestors.mapNotNull { it.parentId }.toSet()
        val stillMissingParents = finalParentIds - finalItemIds

        if (stillMissingParents.isNotEmpty()) {
            log.warn {
                "Still have ${stillMissingParents.size} missing parents after all iterations. " +
                    "These items will appear at root level. Missing parent IDs: ${stillMissingParents.take(10)}..."
            }
        }

        log.info { "Building hierarchy from ${allContentWithAncestors.size} total items (${knownFolderIds.size} folders)" }

        // Build hierarchical structure (pass known folder IDs based on parentType)
        return buildHierarchy(allContentWithAncestors, knownFolderIds)
    }

    /**
     * Get all pages in a space (with pagination and recursive child fetching)
     * This now fetches ALL pages including nested children, not just top-level pages
     */
    fun getAllPages(spaceId: String): List<Page> {
        log.info { "Fetching all pages for space ID: $spaceId (including nested children)" }

        // Step 1: Get top-level items in the space
        val topLevelItems = mutableListOf<Page>()
        var cursor: String? = null

        do {
            val response = confluenceClient.getPagesBySpaceId(spaceId, cursor)
            topLevelItems.addAll(response.results)
            cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            log.info { "Fetched ${response.results.size} top-level items, cursor: $cursor" }
        } while (cursor != null)

        log.info { "Found ${topLevelItems.size} top-level items" }

        // Step 2: Recursively fetch all children for each top-level item
        val allItems = mutableListOf<Page>()
        allItems.addAll(topLevelItems)

        topLevelItems.forEach { item ->
            log.info { "Fetching children for: ${item.title} (ID: ${item.id}, Type: ${item.type})" }
            val children = getAllChildrenRecursively(item)
            allItems.addAll(children)
            if (children.isNotEmpty()) {
                log.info { "  Found ${children.size} descendant(s) under '${item.title}'" }
            }
        }

        log.info { "Total items fetched: ${allItems.size} (${topLevelItems.size} top-level + ${allItems.size - topLevelItems.size} children)" }

        return allItems
    }

    /**
     * Recursively get all children of a page or folder
     * NOTE: Pages can have both page and folder children
     *       Folders can have both page and folder children
     * So we need to try BOTH endpoints for unknown types
     */
    private fun getAllChildrenRecursively(item: Page): List<Page> {
        val allChildren = mutableListOf<Page>()

        // Determine if this is a folder based on type field (if available)
        val knownFolder = item.type?.equals("folder", ignoreCase = true) == true

        if (knownFolder) {
            // Known folder - use folder endpoint (returns pages + folders)
            val directChildren = fetchChildrenFromFolder(item.id, item.title)
            allChildren.addAll(directChildren)
            directChildren.forEach { child ->
                val grandchildren = getAllChildrenRecursively(child)
                allChildren.addAll(grandchildren)
            }
        } else {
            // Unknown type (type=null) - need to check both endpoints
            // Because pages can have folder children and folders can have page children

            // Try 1: Fetch as page (gets page children)
            val pageChildren = fetchChildrenFromPage(item.id, item.title)
            allChildren.addAll(pageChildren)

            // Try 2: Fetch as folder (gets page + folder children)
            val folderChildren = fetchChildrenFromFolder(item.id, item.title)
            allChildren.addAll(folderChildren)

            // Deduplicate (in case item is actually a page with folders under it)
            val uniqueChildren = allChildren.distinctBy { it.id }
            allChildren.clear()
            allChildren.addAll(uniqueChildren)

            // Recursively fetch grandchildren
            uniqueChildren.forEach { child ->
                val grandchildren = getAllChildrenRecursively(child)
                allChildren.addAll(grandchildren)
            }
        }

        return allChildren
    }

    /**
     * Fetch children from a page (child pages only)
     * NOTE: API may not return parentId, so we set it manually
     */
    private fun fetchChildrenFromPage(pageId: String, title: String): List<Page> {
        val children = mutableListOf<Page>()
        try {
            var cursor: String? = null
            do {
                val response = confluenceClient.getPageChildren(pageId, cursor)
                // Ensure parentId is set (API sometimes doesn't include it)
                val childrenWithParent = response.results.map { child ->
                    if (child.parentId == null) {
                        child.copy(parentId = pageId, parentType = "page")
                    } else {
                        child
                    }
                }
                children.addAll(childrenWithParent)
                cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            } while (cursor != null)

            if (children.isNotEmpty()) {
                log.info { "  └─ Page '$title' has ${children.size} child page(s)" }
                // Log first child's parent info for debugging
                children.firstOrNull()?.let { firstChild ->
                    log.debug {
                        "    First child: '${firstChild.title}' → " +
                            "parentId: ${firstChild.parentId}, parentType: ${firstChild.parentType}"
                    }
                }
            }
        } catch (e: Exception) {
            log.debug { "  └─ No page children for '$title': ${e.message}" }
        }
        return children
    }

    /**
     * Fetch children from a folder (pages AND folders)
     * NOTE: API may not return parentId, so we set it manually
     */
    private fun fetchChildrenFromFolder(folderId: String, title: String): List<Page> {
        val children = mutableListOf<Page>()
        try {
            var cursor: String? = null
            do {
                val response = confluenceClient.getFolderChildren(folderId, cursor)
                // Ensure parentId is set (API sometimes doesn't include it)
                val childrenWithParent = response.results.map { child ->
                    if (child.parentId == null) {
                        child.copy(parentId = folderId, parentType = "folder")
                    } else {
                        child
                    }
                }
                children.addAll(childrenWithParent)
                cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            } while (cursor != null)

            if (children.isNotEmpty()) {
                val pageCount = children.count { it.type != "folder" }
                val folderCount = children.count { it.type == "folder" }
                log.info { "  └─ Folder '$title' has ${children.size} child(ren) ($folderCount folder(s), $pageCount page(s))" }
                // Log first child's parent info for debugging
                children.firstOrNull()?.let { firstChild ->
                    log.debug {
                        "    First child: '${firstChild.title}' → " +
                            "parentId: ${firstChild.parentId}, parentType: ${firstChild.parentType}"
                    }
                }
            }
        } catch (e: Exception) {
            log.debug { "  └─ No folder children for '$title': ${e.message}" }
        }
        return children
    }

    /**
     * Build hierarchical structure from all content (pages and folders)
     * Uses explicit folder IDs from parentType and API responses
     */
    private fun buildHierarchy(allContent: List<Page>, explicitFolderIds: Set<String>): ContentHierarchy {
        val folderIds = explicitFolderIds.toSet()

        log.info { "Building hierarchy with ${folderIds.size} identified folders" }

        // Create node map
        val nodeMap = mutableMapOf<String, ContentNode>()

        allContent.forEach { item ->
            val contentType = if (item.id in folderIds) {
                ContentType.FOLDER
            } else {
                ContentType.PAGE
            }

            nodeMap[item.id] = ContentNode(
                id = item.id,
                title = item.title,
                type = contentType,
                parentId = item.parentId,
                body = item.body?.storage?.value
            )

            // Log parent relationships for debugging
            if (item.parentId != null) {
                log.info {
                    "Item: '${item.title}' (ID: ${item.id}) → parent: ${item.parentId} " +
                        "(parentType: ${item.parentType})"
                }
            }
        }

        // Build tree structure
        val rootNodes = mutableListOf<ContentNode>()
        val orphanNodes = mutableListOf<Pair<ContentNode, String>>() // node + missing parent ID

        nodeMap.values.forEach { node ->
            if (node.parentId == null) {
                // Root level content
                rootNodes.add(node)
            } else {
                // Child content - add to parent
                val parent = nodeMap[node.parentId]
                if (parent != null) {
                    parent.children.add(node)
                } else {
                    log.warn {
                        "Orphan node: '${node.title}' (ID: ${node.id}) → " +
                            "parent ID: ${node.parentId} NOT FOUND in nodeMap"
                    }
                    orphanNodes.add(Pair(node, node.parentId!!))
                    // Add as root if parent not found
                    rootNodes.add(node)
                }
            }
        }

        // Sort children (folders first, then alphabetically)
        fun sortChildren(node: ContentNode) {
            node.children.sortWith(
                compareBy<ContentNode> { it.type != ContentType.FOLDER } // Folders first
                    .thenBy { it.title } // Then alphabetically
            )
            node.children.forEach { sortChildren(it) }
        }
        rootNodes.forEach { sortChildren(it) }

        val finalFolderCount = nodeMap.values.count { it.type == ContentType.FOLDER }
        val finalPageCount = nodeMap.values.count { it.type == ContentType.PAGE }

        if (orphanNodes.isNotEmpty()) {
            log.warn {
                "Found ${orphanNodes.size} orphan nodes (parentId exists but parent not in map):"
            }
            orphanNodes.take(20).forEach { (node, parentId) ->
                log.warn { "  - '${node.title}' (${node.id}) → missing parent: $parentId" }
            }
        }

        log.info {
            "Built hierarchy: ${rootNodes.size} root nodes, " +
                "$finalFolderCount folders, $finalPageCount pages"
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
