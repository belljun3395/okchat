package com.okestro.okchat.confluence.service

import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.client.Page
import com.okestro.okchat.confluence.model.ContentHierarchy
import com.okestro.okchat.confluence.model.ContentNode
import com.okestro.okchat.confluence.model.ContentType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Responsible for building hierarchical structure from flat list of pages
 */
@Component
class HierarchyBuilder(
    private val confluenceClient: ConfluenceClient
) {
    /**
     * Build hierarchical structure from collected content
     */
    fun buildHierarchy(allContent: List<Page>, spaceId: String): ContentHierarchy {
        log.info { "Building hierarchy from ${allContent.size} items" }

        // Identify folders using parentType field
        val folderIds = identifyFolders(allContent)
        log.info { "Identified ${folderIds.size} folders" }

        // Fill missing parents
        val completeContent = fillMissingParents(allContent, folderIds, spaceId)
        log.info { "Complete content: ${completeContent.size} items" }

        // Build tree structure
        return buildTree(completeContent, folderIds)
    }

    /**
     * Identify folders from explicit type and parentType fields
     */
    private fun identifyFolders(allContent: List<Page>): MutableSet<String> {
        val folderIdsFromChildren = allContent
            .filter { it.parentType?.equals("folder", ignoreCase = true) == true }
            .mapNotNull { it.parentId }
            .toSet()

        val explicitFolderIds = allContent
            .filter { it.type?.equals("folder", ignoreCase = true) == true }
            .map { it.id }
            .toSet()

        val knownFolderIds = (folderIdsFromChildren + explicitFolderIds).toMutableSet()

        log.info {
            "Folder identification: ${explicitFolderIds.size} explicit + " +
                "${folderIdsFromChildren.size} from parentType = ${knownFolderIds.size} total"
        }

        return knownFolderIds
    }

    /**
     * Fill missing parent nodes by fetching them from API
     */
    private fun fillMissingParents(
        allContent: List<Page>,
        knownFolderIds: MutableSet<String>,
        spaceId: String
    ): List<Page> {
        val allItemIds = allContent.map { it.id }.toSet()
        val allParentIds = allContent.mapNotNull { it.parentId }.toSet()
        var missingParentIds = allParentIds - allItemIds

        log.info { "Initial missing parents: ${missingParentIds.size}" }

        val fetchedAncestors = mutableListOf<Page>()
        var safety = 0

        while (missingParentIds.isNotEmpty() && safety++ < 10) {
            log.info { "Fetching ${missingParentIds.size} missing parent(s) (iteration $safety)" }

            val newlyFetched = missingParentIds.mapNotNull { parentId ->
                fetchMissingParent(parentId, knownFolderIds, spaceId)
            }

            if (newlyFetched.isEmpty()) {
                log.warn { "Could not fetch any of the ${missingParentIds.size} missing parents" }
                break
            }

            fetchedAncestors.addAll(newlyFetched)

            // Recompute missing parents
            val current = allContent + fetchedAncestors
            val currentIds = current.map { it.id }.toSet()
            val currentParentIds = current.mapNotNull { it.parentId }.toSet()
            val newMissingParentIds = currentParentIds - currentIds

            if (newMissingParentIds == missingParentIds) {
                log.warn { "No progress in fetching parents - stopping" }
                break
            }

            missingParentIds = newMissingParentIds
        }

        log.info { "Fetched ${fetchedAncestors.size} missing ancestor(s) across $safety iteration(s)" }

        val finalContent = allContent + fetchedAncestors
        val finalItemIds = finalContent.map { it.id }.toSet()
        val finalParentIds = finalContent.mapNotNull { it.parentId }.toSet()
        val stillMissingParents = finalParentIds - finalItemIds

        if (stillMissingParents.isNotEmpty()) {
            log.warn {
                "Still have ${stillMissingParents.size} missing parents. " +
                    "These items will appear at root level. " +
                    "Missing IDs: ${stillMissingParents.take(10)}..."
            }
        }

        return finalContent
    }

    /**
     * Fetch a single missing parent
     */
    private fun fetchMissingParent(
        parentId: String,
        knownFolderIds: MutableSet<String>,
        spaceId: String
    ): Page? {
        // Try folder first
        return try {
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

    /**
     * Build tree structure from flat list
     */
    private fun buildTree(allContent: List<Page>, folderIds: Set<String>): ContentHierarchy {
        log.info { "Building tree with ${folderIds.size} folders" }

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
                log.debug {
                    "Item: '${item.title}' (ID: ${item.id}) → parent: ${item.parentId} " +
                        "(parentType: ${item.parentType})"
                }
            }
        }

        // Build tree structure
        val rootNodes = mutableListOf<ContentNode>()
        val orphanNodes = mutableListOf<Pair<ContentNode, String>>()

        nodeMap.values.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                if (parent != null) {
                    parent.children.add(node)
                } else {
                    log.warn {
                        "Orphan node: '${node.title}' (ID: ${node.id}) → " +
                            "parent ID: ${node.parentId} NOT FOUND"
                    }
                    orphanNodes.add(Pair(node, node.parentId!!))
                    rootNodes.add(node)
                }
            }
        }

        // Sort children (folders first, then alphabetically)
        sortChildren(rootNodes)

        // Log results
        val finalFolderCount = nodeMap.values.count { it.type == ContentType.FOLDER }
        val finalPageCount = nodeMap.values.count { it.type == ContentType.PAGE }

        if (orphanNodes.isNotEmpty()) {
            log.warn { "Found ${orphanNodes.size} orphan nodes:" }
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

    /**
     * Sort children recursively (folders first, then alphabetically)
     */
    private fun sortChildren(nodes: List<ContentNode>) {
        fun sortNode(node: ContentNode) {
            node.children.sortWith(
                compareBy<ContentNode> { it.type != ContentType.FOLDER }
                    .thenBy { it.title }
            )
            node.children.forEach { sortNode(it) }
        }
        nodes.forEach { sortNode(it) }
    }
}
