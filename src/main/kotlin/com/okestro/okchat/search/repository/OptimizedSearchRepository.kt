package com.okestro.okchat.search.repository

import com.okestro.okchat.search.model.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * Optimized repository for bulk search operations
 * Uses native queries and batch processing for better performance
 */
@Repository
class OptimizedSearchRepository {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    /**
     * Bulk fetch documents by paths with optimized query
     */
    @Transactional(readOnly = true)
    suspend fun findDocumentsByPathsOptimized(paths: List<String>): List<Document> = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext emptyList()
        
        val query = entityManager.createQuery(
            """
            SELECT d FROM Document d 
            WHERE d.metadata.path IN :paths
            AND d.metadata.deleted = false
            ORDER BY d.metadata.lastModified DESC
            """,
            Document::class.java
        )
        
        query.setParameter("paths", paths)
        query.setHint("org.hibernate.fetchSize", 50)
        query.setHint("org.hibernate.readOnly", true)
        
        query.resultList
    }
    
    /**
     * Find documents with path prefix using index
     */
    @Transactional(readOnly = true)
    suspend fun findDocumentsByPathPrefix(pathPrefix: String): List<Document> = withContext(Dispatchers.IO) {
        val query = entityManager.createQuery(
            """
            SELECT d FROM Document d 
            WHERE d.metadata.path LIKE :pathPrefix
            AND d.metadata.deleted = false
            ORDER BY d.metadata.lastModified DESC
            """,
            Document::class.java
        )
        
        query.setParameter("pathPrefix", "$pathPrefix%")
        query.setHint("org.hibernate.fetchSize", 100)
        query.setHint("org.hibernate.readOnly", true)
        query.setMaxResults(1000) // Prevent memory issues
        
        query.resultList
    }
    
    /**
     * Count documents by space with optimized query
     */
    @Transactional(readOnly = true)
    suspend fun countDocumentsBySpace(spaceKey: String): Long = withContext(Dispatchers.IO) {
        val query = entityManager.createQuery(
            """
            SELECT COUNT(d) FROM Document d 
            WHERE d.metadata.spaceKey = :spaceKey
            AND d.metadata.deleted = false
            """,
            Long::class.java
        )
        
        query.setParameter("spaceKey", spaceKey)
        query.singleResult
    }
    
    /**
     * Batch update document metadata
     */
    @Transactional
    suspend fun batchUpdateDocumentMetadata(updates: List<DocumentMetadataUpdate>) = withContext(Dispatchers.IO) {
        if (updates.isEmpty()) return@withContext
        
        val batchSize = 25
        var count = 0
        
        updates.forEach { update ->
            val query = entityManager.createQuery(
                """
                UPDATE Document d 
                SET d.metadata.lastModified = :lastModified,
                    d.metadata.version = d.metadata.version + 1
                WHERE d.id = :id
                """
            )
            
            query.setParameter("id", update.documentId)
            query.setParameter("lastModified", update.lastModified)
            query.executeUpdate()
            
            count++
            if (count % batchSize == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }
        
        if (count % batchSize != 0) {
            entityManager.flush()
            entityManager.clear()
        }
        
        log.info { "Batch updated $count document metadata records" }
    }
    
    data class DocumentMetadataUpdate(
        val documentId: String,
        val lastModified: Long
    )
}