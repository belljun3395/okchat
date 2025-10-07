package com.okestro.okchat.permission.service

import com.okestro.okchat.permission.controller.DocumentNode
import com.okestro.okchat.permission.entity.DocumentPermission
import com.okestro.okchat.permission.entity.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPermissionRepository
import com.okestro.okchat.search.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * Service for document permission management and filtering
 *
 * Responsibilities:
 * - Grant/revoke document permissions
 * - Filter search results based on user permissions
 * - Check if user has access to specific documents
 */
@Service
class PermissionService(
    private val documentPermissionRepository: DocumentPermissionRepository,
    private val openSearchClient: OpenSearchClient,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String
) {

    /**
     * Filter search results to only include documents the user has permission to access
     *
     * Checks both document-specific and path-based permissions:
     * 1. Direct document ID match
     * 2. Document path matches any granted path prefix
     *
     * @param results Original search results
     * @param userId User ID to check permissions for
     * @return Filtered list containing only accessible documents
     */
    fun filterSearchResults(results: List<SearchResult>, userId: Long): List<SearchResult> {
        if (results.isEmpty()) {
            return emptyList()
        }

        log.debug { "[PermissionFilter] Filtering ${results.size} results for user_id=$userId" }

        // Get all accessible document IDs and paths for user
        val accessibleDocIds = getAccessibleDocumentIds(userId)
        val accessiblePaths = getAccessiblePaths(userId)

        log.debug {
            "[PermissionFilter] User has access to ${accessibleDocIds.size} specific documents " +
                "and ${accessiblePaths.size} path-based permissions"
        }

        // Filter results - check both document ID and path
        val filtered = results.filter { result ->
            val docId = extractBaseDocumentId(result.id)
            val docPath = result.path

            // Check 1: Direct document ID match
            val hasDocIdPermission = accessibleDocIds.contains(docId)

            // Check 2: Path-based permission (document path starts with any granted path)
            val hasPathPermission = accessiblePaths.any { grantedPath ->
                isPathMatching(docPath, grantedPath)
            }

            hasDocIdPermission || hasPathPermission
        }

        val filteredCount = results.size - filtered.size
        if (filteredCount > 0) {
            log.info { "[PermissionFilter] Filtered out $filteredCount documents (${results.size} -> ${filtered.size})" }
        }

        return filtered
    }

    /**
     * Check if user has permission to access a specific document
     */
    fun hasPermission(userId: Long, documentId: String): Boolean {
        val baseDocId = extractBaseDocumentId(documentId)
        return documentPermissionRepository.existsByUserIdAndDocumentIdAndPermissionLevel(
            userId = userId,
            documentId = baseDocId,
            permissionLevel = PermissionLevel.READ
        )
    }

    /**
     * Get all document IDs accessible by a user (document-specific permissions)
     * This is used for efficient batch filtering
     */
    fun getAccessibleDocumentIds(userId: Long): Set<String> {
        return documentPermissionRepository.findDocumentIdsByUserId(userId)
            .filterNotNull()
            .toSet()
    }

    /**
     * Get all accessible paths for a user (path-based permissions)
     */
    fun getAccessiblePaths(userId: Long): Set<String> {
        return documentPermissionRepository.findPathBasedPermissionsByUserId(userId)
            .mapNotNull { it.documentPath }
            .toSet()
    }

    /**
     * Check if document path matches granted path
     * Supports hierarchical matching: "A > B > C" matches granted path "A > B"
     */
    private fun isPathMatching(documentPath: String, grantedPath: String): Boolean {
        if (documentPath.isEmpty() || grantedPath.isEmpty()) return false

        // Exact match
        if (documentPath == grantedPath) return true

        // Hierarchical match: document path starts with granted path
        // Example: "팀회의 > 2025 > 1월 > 회의록" starts with "팀회의 > 2025"
        return documentPath.startsWith("$grantedPath >")
    }

    /**
     * Grant READ permission to a user for a specific document
     */
    @Transactional("transactionManager")
    fun grantDocumentPermission(
        userId: Long,
        documentId: String,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): DocumentPermission {
        val baseDocId = extractBaseDocumentId(documentId)

        // Check if permission already exists
        val existing = documentPermissionRepository.findByUserIdAndDocumentId(userId, baseDocId)
        if (existing != null) {
            log.debug { "Permission already exists: user_id=$userId, document_id=$baseDocId" }
            return existing
        }

        // Fetch document metadata from OpenSearch to populate path and spaceKey
        val documentContent = getDocumentContent(baseDocId)
        val effectivePath = documentContent?.path
        val effectiveSpaceKey = spaceKey ?: documentContent?.spaceKey

        val permission = DocumentPermission(
            userId = userId,
            documentId = baseDocId,
            documentPath = effectivePath,
            spaceKey = effectiveSpaceKey,
            permissionLevel = PermissionLevel.READ,
            grantedBy = grantedBy
        )

        return documentPermissionRepository.save(permission).also {
            log.info { "Document permission granted: user_id=$userId, document_id=$baseDocId, path=$effectivePath, space_key=$effectiveSpaceKey" }
        }
    }

    /**
     * Grant READ permission to a user for all documents under a path
     */
    @Transactional("transactionManager")
    fun grantPathPermission(
        userId: Long,
        documentPath: String,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): DocumentPermission {
        // Check if permission already exists
        if (documentPermissionRepository.existsByUserIdAndDocumentPath(userId, documentPath)) {
            val existing = documentPermissionRepository.findPathBasedPermissionsByUserId(userId)
                .find { it.documentPath == documentPath }
            if (existing != null) {
                log.debug { "Path permission already exists: user_id=$userId, path=$documentPath" }
                return existing
            }
        }

        val permission = DocumentPermission(
            userId = userId,
            documentId = null,
            documentPath = documentPath,
            spaceKey = spaceKey,
            permissionLevel = PermissionLevel.READ,
            grantedBy = grantedBy
        )

        return documentPermissionRepository.save(permission).also {
            log.info { "Path permission granted: user_id=$userId, path=$documentPath, space_key=$spaceKey" }
        }
    }

    /**
     * Grant READ permission (backward compatibility - delegates to grantDocumentPermission)
     */
    @Transactional("transactionManager")
    fun grantPermission(
        userId: Long,
        documentId: String,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): DocumentPermission {
        return grantDocumentPermission(userId, documentId, spaceKey, grantedBy)
    }

    /**
     * Revoke permission
     */
    @Transactional("transactionManager")
    fun revokePermission(userId: Long, documentId: String) {
        val baseDocId = extractBaseDocumentId(documentId)
        documentPermissionRepository.deleteByUserIdAndDocumentId(userId, baseDocId)
        log.info { "Permission revoked: user_id=$userId, document_id=$baseDocId" }
    }

    /**
     * Revoke path-based permission
     * Also revokes all document-specific permissions under the same path (cascade)
     */
    @Transactional("transactionManager")
    fun revokePathPermission(userId: Long, documentPath: String) {
        // Delete the path-based permission
        documentPermissionRepository.deleteByUserIdAndDocumentPath(userId, documentPath)
        log.info { "Path permission revoked: user_id=$userId, path=$documentPath" }

        // Get all documents under this path
        val docIdsToRevoke = getDocumentsByPath(documentPath).map { it.id }

        // Also delete document-specific permissions for documents under this path
        if (docIdsToRevoke.isNotEmpty()) {
            documentPermissionRepository.deleteByUserIdAndDocumentIdIn(userId, docIdsToRevoke)
            log.info { "Cascading revoke of ${docIdsToRevoke.size} document-specific permissions for user_id=$userId under path: $documentPath" }
        }

        // Additionally, delete all document-specific permissions that have documentPath matching this path
        // This handles permissions created with the new grantDocumentPermission that includes documentPath
        val userPermissions = documentPermissionRepository.findByUserId(userId)
        val permissionsToDelete = userPermissions.filter { perm ->
            perm.documentId != null && perm.documentPath != null &&
                isPathMatching(perm.documentPath!!, documentPath)
        }

        if (permissionsToDelete.isNotEmpty()) {
            val idsToDelete = permissionsToDelete.mapNotNull { it.documentId }
            documentPermissionRepository.deleteByUserIdAndDocumentIdIn(userId, idsToDelete)
            log.info { "Cascading revoke of ${idsToDelete.size} additional document-specific permissions with matching path for user_id=$userId" }
        }
    }

    /**
     * Revoke multiple document-based permissions
     */
    @Transactional("transactionManager")
    fun revokeBulkDocumentPermissions(userId: Long, documentIds: List<String>) {
        documentPermissionRepository.deleteByUserIdAndDocumentIdIn(userId, documentIds)
        log.info { "Bulk document permissions revoked: user_id=$userId, count=${documentIds.size}" }
    }

    /**
     * Revoke multiple path-based permissions
     */
    @Transactional("transactionManager")
    fun revokeBulkPathPermissions(userId: Long, documentPaths: List<String>) {
        val docIdsToRevoke = documentPaths.flatMap { path ->
            getDocumentsByPath(path).map { it.id }
        }.distinct()

        documentPermissionRepository.deleteByUserIdAndDocumentPathIn(userId, documentPaths)
        log.info { "Bulk path permissions revoked: user_id=$userId, count=${documentPaths.size}" }

        if (docIdsToRevoke.isNotEmpty()) {
            documentPermissionRepository.deleteByUserIdAndDocumentIdIn(userId, docIdsToRevoke)
            log.info { "Cascading revoke of ${docIdsToRevoke.size} document permissions for user_id=$userId under paths: $documentPaths" }
        }
    }

    /**
     * Grant permissions to multiple documents at once (bulk operation)
     * Useful for granting access to all documents in a space
     */
    @Transactional("transactionManager")
    fun grantBulkDocumentPermissions(
        userId: Long,
        documentIds: List<String>,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): Int {
        var grantedCount = 0

        documentIds.forEach { documentId ->
            try {
                grantDocumentPermission(userId, documentId, spaceKey, grantedBy)
                grantedCount++
            } catch (e: Exception) {
                log.warn { "Failed to grant permission: user_id=$userId, document_id=$documentId, error=${e.message}" }
            }
        }

        log.info { "Bulk document permissions granted: user_id=$userId, granted=$grantedCount/${documentIds.size}" }
        return grantedCount
    }

    /**
     * Grant permissions to multiple paths at once
     */
    @Transactional("transactionManager")
    fun grantBulkPathPermissions(
        userId: Long,
        documentPaths: List<String>,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): Int {
        var grantedCount = 0

        documentPaths.forEach { path ->
            try {
                grantPathPermission(userId, path, spaceKey, grantedBy)
                grantedCount++
            } catch (e: Exception) {
                log.warn { "Failed to grant path permission: user_id=$userId, path=$path, error=${e.message}" }
            }
        }

        log.info { "Bulk path permissions granted: user_id=$userId, granted=$grantedCount/${documentPaths.size}" }
        return grantedCount
    }

    /**
     * Backward compatibility wrapper
     */
    @Transactional("transactionManager")
    fun grantBulkPermissions(
        userId: Long,
        documentIds: List<String>,
        spaceKey: String? = null,
        grantedBy: Long? = null
    ): Int {
        return grantBulkDocumentPermissions(userId, documentIds, spaceKey, grantedBy)
    }

    /**
     * Revoke all permissions for a user
     */
    @Transactional("transactionManager")
    fun revokeAllPermissionsForUser(userId: Long) {
        documentPermissionRepository.deleteByUserId(userId)
        log.info { "All permissions revoked for user: user_id=$userId" }
    }

    /**
     * Revoke all permissions for a document
     */
    @Transactional("transactionManager")
    fun revokeAllPermissionsForDocument(documentId: String) {
        val baseDocId = extractBaseDocumentId(documentId)
        documentPermissionRepository.deleteByDocumentId(baseDocId)
        log.info { "All permissions revoked for document: document_id=$baseDocId" }
    }

    /**
     * Get all permissions for a user
     */
    fun getUserPermissions(userId: Long): List<DocumentPermission> {
        return documentPermissionRepository.findByUserId(userId)
    }

    /**
     * Get all permissions for a document
     */
    fun getDocumentPermissions(documentId: String): List<DocumentPermission> {
        val baseDocId = extractBaseDocumentId(documentId)
        return documentPermissionRepository.findByDocumentId(baseDocId)
    }

    /**
     * Get all permissions for a path
     */
    fun getPathPermissions(documentPath: String): List<DocumentPermission> {
        return documentPermissionRepository.findByDocumentPath(documentPath)
    }

    /**
     * Get all unique document paths (for path browsing)
     */
    fun getAllDocumentPaths(): List<String> {
        return documentPermissionRepository.findAllDistinctDocumentPaths()
            .filterNotNull()
    }

    /**
     * Get all document IDs under a specific path from OpenSearch
     * This is useful for bulk permission operations
     *
     * @param documentPath The path to search for (e.g., "팀회의 > 2025")
     * @return List of document IDs that match or are under the specified path
     */
    fun getDocumentsByPath(documentPath: String): List<DocumentNode> {
        val documents = mutableMapOf<String, DocumentNode>() // Use map to handle chunks and keep unique docs

        try {
            var from = 0
            val size = 100

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .query { q ->
                            q.bool { b ->
                                b.should { sh ->
                                    // Exact match
                                    sh.term { t ->
                                        t.field("metadata.path")
                                            .value(org.opensearch.client.opensearch._types.FieldValue.of(documentPath))
                                    }
                                }
                                    .should { sh ->
                                        // Prefix match for hierarchical paths
                                        sh.prefix { p ->
                                            p.field("metadata.path")
                                                .value("$documentPath >")
                                        }
                                    }
                                    .minimumShouldMatch("1")
                            }
                        }
                        .source { src -> src.filter { f -> f.includes(listOf("id", "metadata.id", "metadata.title", "metadata.path")) } }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                hits.forEach { hit ->
                    val source = hit.source()
                    if (source != null) {
                        // Get Confluence ID from metadata.id field (flattened field in OpenSearch)
                        val confluenceId = source["metadata.id"]?.toString()

                        if (confluenceId != null) {
                            val baseId = extractBaseDocumentId(confluenceId)

                            if (!documents.containsKey(baseId)) {
                                val title = source["metadata.title"]?.toString() ?: "Untitled"
                                val path = source["metadata.path"]?.toString() ?: ""

                                documents[baseId] = DocumentNode(
                                    id = baseId,
                                    title = title,
                                    path = path
                                )
                            }
                        }
                    }
                }

                if (hits.size < size) break
                from += size
            }

            log.info { "Found ${documents.size} documents under path: $documentPath" }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch documents by path: $documentPath, error=${e.message}" }
        }

        return documents.values.toList().sortedBy { it.title }
    }

    /**
     * Get all unique paths from OpenSearch
     * This queries the actual indexed documents to get available paths
     */
    fun getAllDocumentPathsFromIndex(): List<String> {
        val paths = mutableSetOf<String>()

        try {
            var from = 0
            val size = 200

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .source { src -> src.filter { f -> f.includes(listOf("metadata.path")) } }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                // Extract paths
                hits.forEach { hit ->
                    val source = hit.source()
                    // Handle both nested and flat field structures
                    val path = when {
                        source?.containsKey("metadata.path") == true -> source["metadata.path"]?.toString()
                        else -> {
                            val metadata = source?.get("metadata") as? Map<*, *>
                            metadata?.get("path")?.toString()
                        }
                    }
                    path?.let { if (it.isNotBlank()) paths.add(it) }
                }

                // Check if there are more pages
                if (hits.size < size) break
                from += size
            }

            log.info { "Found ${paths.size} unique paths from index" }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch paths from index: ${e.message}" }
        }

        return paths.toList().sorted()
    }

    /**
     * Get document content for preview
     */
    fun getDocumentContent(documentId: String): com.okestro.okchat.permission.controller.DocumentContentResponse? {
        val baseDocId = extractBaseDocumentId(documentId)

        try {
            val searchResponse = openSearchClient.search({ s ->
                s.index(indexName)
                    .query { q ->
                        q.bool { b ->
                            b.should { sh ->
                                sh.term { t ->
                                    t.field("id")
                                        .value(org.opensearch.client.opensearch._types.FieldValue.of(baseDocId))
                                }
                            }
                                .should { sh ->
                                    sh.term { t ->
                                        t.field("metadata.id")
                                            .value(org.opensearch.client.opensearch._types.FieldValue.of(baseDocId))
                                    }
                                }
                                .minimumShouldMatch("1")
                        }
                    }
                    .size(10)
            }, Map::class.java)

            val hits = searchResponse.hits().hits()
            if (hits.isEmpty()) {
                log.warn { "Document not found: $baseDocId" }
                return null
            }

            // Combine content from all chunks
            val contentBuilder = StringBuilder()
            var title = ""
            var path = ""
            var spaceKey: String? = null

            hits.forEach { hit ->
                val source = hit.source() ?: return@forEach

                // Get content
                val content = source["content"]?.toString() ?: ""
                if (content.isNotBlank()) {
                    contentBuilder.append(content).append("\n")
                }

                // Get metadata from first hit (flattened fields in OpenSearch)
                if (title.isEmpty()) {
                    title = source["metadata.title"]?.toString() ?: ""
                    path = source["metadata.path"]?.toString() ?: ""
                    spaceKey = source["metadata.spaceKey"]?.toString()
                }
            }

            return com.okestro.okchat.permission.controller.DocumentContentResponse(
                documentId = baseDocId,
                title = title,
                content = contentBuilder.toString().trim(),
                path = path,
                spaceKey = spaceKey
            )
        } catch (e: Exception) {
            log.error(e) { "Failed to get document content: $baseDocId" }
            return null
        }
    }

    /**
     * Build folder hierarchy from document paths
     */
    fun buildFolderHierarchy(): List<com.okestro.okchat.permission.controller.FolderNode> {
        try {
            // Get all documents with their paths
            val documents = mutableMapOf<String, MutableList<Pair<String, String>>>() // path -> list of (id, title)

            var from = 0
            val size = 200

            while (true) {
                val searchResponse = openSearchClient.search({ s ->
                    s.index(indexName)
                        .from(from)
                        .size(size)
                        .source { src -> src.filter { f -> f.includes(listOf("id", "metadata.id", "metadata.title", "metadata.path")) } }
                }, Map::class.java)

                val hits = searchResponse.hits().hits()
                if (hits.isEmpty()) break

                hits.forEach { hit ->
                    val source = hit.source() ?: return@forEach

                    // Get Confluence ID from metadata.id field (flattened field in OpenSearch)
                    val confluenceId = source["metadata.id"]?.toString()
                    val path = source["metadata.path"]?.toString() ?: ""
                    val title = source["metadata.title"]?.toString() ?: "Untitled"

                    if (path.isNotBlank() && confluenceId != null) {
                        val baseId = extractBaseDocumentId(confluenceId)
                        documents.getOrPut(path) { mutableListOf() }.add(baseId to title)
                    }
                }

                if (hits.size < size) break
                from += size
            }

            // Remove duplicates (same document might appear multiple times due to chunks)
            documents.forEach { (_, docs) ->
                docs.distinctBy { it.first }
            }

            // Build hierarchy
            return buildHierarchyRecursive(documents)
        } catch (e: Exception) {
            log.error(e) { "Failed to build folder hierarchy: ${e.message}" }
            return emptyList()
        }
    }

    private fun buildHierarchyRecursive(
        documents: Map<String, List<Pair<String, String>>>,
        parentPath: String = ""
    ): List<com.okestro.okchat.permission.controller.FolderNode> {
        val folders = mutableMapOf<String, MutableList<String>>() // folderName -> list of full paths

        // Group paths by immediate child folder
        documents.keys.forEach { path ->
            if (parentPath.isEmpty()) {
                // Root level
                val parts = path.split(" > ")
                if (parts.isNotEmpty()) {
                    val folderName = parts[0]
                    folders.getOrPut(folderName) { mutableListOf() }.add(path)
                }
            } else {
                // Check if this path is under parent
                if (path.startsWith("$parentPath > ")) {
                    val remaining = path.substring(parentPath.length + 3) // +3 for " > "
                    val parts = remaining.split(" > ")
                    if (parts.isNotEmpty()) {
                        val folderName = parts[0]
                        folders.getOrPut(folderName) { mutableListOf() }.add(path)
                    }
                }
            }
        }

        // Build folder nodes
        return folders.map { (folderName, paths) ->
            val currentPath = if (parentPath.isEmpty()) folderName else "$parentPath > $folderName"

            // Get documents directly in this folder
            val docsInFolder = documents[currentPath]?.map { (id, title) ->
                com.okestro.okchat.permission.controller.DocumentNode(
                    id = id,
                    title = title,
                    path = currentPath
                )
            }?.distinctBy { it.id } ?: emptyList()

            // Get child folders
            val childPaths = paths.filter { it != currentPath }
            val childDocuments = childPaths.associateWith { documents[it] ?: emptyList() }
            val children = if (childDocuments.isNotEmpty()) {
                buildHierarchyRecursive(documents, currentPath)
            } else {
                emptyList()
            }

            com.okestro.okchat.permission.controller.FolderNode(
                name = folderName,
                path = currentPath,
                children = children,
                documents = docsInFolder
            )
        }.sortedBy { it.name }
    }

    /**
     * Extract base document ID from potentially chunked document ID
     * Example: "12345678_chunk_0" -> "12345678"
     */
    private fun extractBaseDocumentId(documentId: String): String {
        return if (documentId.contains("_chunk_")) {
            documentId.substringBefore("_chunk_")
        } else {
            documentId
        }
    }
}
