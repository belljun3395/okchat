package com.okestro.okchat.confluence.service

import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.client.dto.Page
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Responsible for collecting all content (pages and folders) from Confluence space
 */
@Component
class ContentCollectorService(
    private val confluenceClient: ConfluenceClient
) {
    /**
     * Collect all pages and folders in a space, including nested children
     */
    suspend fun collectAllContent(spaceId: String): List<Page> {
        log.info { "Collecting all content for space ID: $spaceId" }

        // Step 1: Get top-level items
        val topLevelItems = fetchTopLevelItems(spaceId)
        log.info { "Found ${topLevelItems.size} top-level items" }

        // Step 2: Recursively fetch all children
        val allItems = mutableListOf<Page>()
        allItems.addAll(topLevelItems)

        topLevelItems.forEach { item ->
            log.info { "Fetching children for: ${item.title} (ID: ${item.id}, Type: ${item.type})" }
            val children = collectChildrenRecursively(item)
            allItems.addAll(children)
            if (children.isNotEmpty()) {
                log.info { "Found ${children.size} descendant(s) under '${item.title}'" }
            }
        }

        log.info { "Total items collected: ${allItems.size} (${topLevelItems.size} top-level + ${allItems.size - topLevelItems.size} children)" }

        return allItems
    }

    /**
     * Fetch top-level items in a space
     */
    private suspend fun fetchTopLevelItems(spaceId: String): List<Page> {
        val items = mutableListOf<Page>()
        var cursor: String? = null

        do {
            val response = withContext(Dispatchers.IO + MDCContext()) {
                confluenceClient.getPagesBySpaceId(spaceId, cursor)
            }
            items.addAll(response.results)
            cursor = response._links?.next?.substringAfter("cursor=")?.substringBefore("&")
            log.info { "Fetched ${response.results.size} top-level items, cursor: $cursor" }
        } while (cursor != null)

        return items
    }

    /**
     * Recursively collect all children of a page or folder
     * Handles mixed hierarchies (pages can have folders, folders can have pages)
     */
    private suspend fun collectChildrenRecursively(item: Page): List<Page> {
        val allChildren = mutableListOf<Page>()

        val knownFolder = item.type?.equals("folder", ignoreCase = true) == true

        if (knownFolder) {
            // Known folder - use folder endpoint
            val directChildren = fetchFolderChildren(item.id, item.title)
            allChildren.addAll(directChildren)
            directChildren.forEach { child ->
                val grandchildren = collectChildrenRecursively(child)
                allChildren.addAll(grandchildren)
            }
        } else {
            // Unknown type - try both endpoints for mixed hierarchies
            val pageChildren = fetchPageChildren(item.id, item.title)
            allChildren.addAll(pageChildren)

            val folderChildren = fetchFolderChildren(item.id, item.title)
            allChildren.addAll(folderChildren)

            // Deduplicate by ID
            val uniqueChildren = allChildren.distinctBy { it.id }
            allChildren.clear()
            allChildren.addAll(uniqueChildren)

            // Recursively collect grandchildren
            uniqueChildren.forEach { child ->
                val grandchildren = collectChildrenRecursively(child)
                allChildren.addAll(grandchildren)
            }
        }

        return allChildren
    }

    /**
     * Fetch children from a page
     * Sets parentId if API doesn't provide it
     */
    private suspend fun fetchPageChildren(pageId: String, title: String): List<Page> {
        val children = mutableListOf<Page>()
        try {
            var cursor: String? = null
            do {
                val response = withContext(Dispatchers.IO + MDCContext()) {
                    confluenceClient.getPageChildren(pageId, cursor)
                }
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
                log.info { "└─ Page '$title' has ${children.size} child page(s)" }
                children.firstOrNull()?.let { firstChild ->
                    log.debug {
                        "First child: '${firstChild.title}' → " +
                            "parentId: ${firstChild.parentId}, parentType: ${firstChild.parentType}"
                    }
                }
            }
        } catch (e: Exception) {
            log.debug { "└─ No page children for '$title': ${e.message}" }
        }
        return children
    }

    /**
     * Fetch children from a folder
     * Sets parentId if API doesn't provide it
     */
    private suspend fun fetchFolderChildren(folderId: String, title: String): List<Page> {
        val children = mutableListOf<Page>()
        try {
            var cursor: String? = null
            do {
                val response = withContext(Dispatchers.IO + MDCContext()) {
                    confluenceClient.getFolderChildren(folderId, cursor)
                }
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
                log.info { "└─ Folder '$title' has ${children.size} child(ren) ($folderCount folder(s), $pageCount page(s))" }
                children.firstOrNull()?.let { firstChild ->
                    log.debug {
                        "First child: '${firstChild.title}' → " +
                            "parentId: ${firstChild.parentId}, parentType: ${firstChild.parentType}"
                    }
                }
            }
        } catch (e: Exception) {
            log.debug { "└─ No folder children for '$title': ${e.message}" }
        }
        return children
    }
}
