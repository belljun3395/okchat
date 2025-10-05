package com.okestro.okchat.confluence.service

import com.okestro.okchat.confluence.client.ConfluenceClient
import com.okestro.okchat.confluence.model.ContentHierarchy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Facade service for Confluence operations
 * Delegates to specialized components for better separation of concerns
 */
@Service
class ConfluenceService(
    private val confluenceClient: ConfluenceClient,
    private val contentCollector: ContentCollector,
    private val hierarchyBuilder: HierarchyBuilder
) {
    /**
     * Get space ID by space key
     */
    fun getSpaceIdByKey(spaceKey: String): String {
        log.info { "Getting space ID for key: $spaceKey" }
        val response = confluenceClient.getSpaceByKey(spaceKey)

        if (response.results.isEmpty()) {
            throw IllegalArgumentException("Space not found: $spaceKey")
        }

        val spaceId = response.results.first().id
        log.info { "Found space ID: $spaceId" }
        return spaceId
    }

    /**
     * Get content hierarchy for a space
     * This is the main entry point for fetching and organizing Confluence content
     */
    fun getSpaceContentHierarchy(spaceId: String): ContentHierarchy {
        log.info { "Fetching content hierarchy for space: $spaceId" }

        // Step 1: Collect all content (pages and folders)
        val allContent = contentCollector.collectAllContent(spaceId)

        // Step 2: Build hierarchical structure
        val hierarchy = hierarchyBuilder.buildHierarchy(allContent, spaceId)

        // Step 3: Log statistics
        val stats = hierarchy.getStatistics()
        log.info {
            "Hierarchy complete: ${stats.totalNodes} nodes " +
                "(${stats.folders} folders, ${stats.pages} pages), " +
                "max depth: ${stats.maxDepth}, " +
                "avg children/node: ${"%.1f".format(stats.avgChildrenPerNode)}"
        }

        return hierarchy
    }
}
