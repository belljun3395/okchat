package com.okestro.okchat.permission.application

import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseIn
import com.okestro.okchat.permission.application.dto.FilterSearchResultsUseCaseOut
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class FilterSearchResultsUseCase(
    private val documentPathPermissionRepository: DocumentPathPermissionRepository,
    private val userClient: UserClient,
    private val knowledgeMemberClient: KnowledgeMemberClient
) {

    suspend fun execute(useCaseIn: FilterSearchResultsUseCaseIn): FilterSearchResultsUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val (results, userId) = useCaseIn
            if (results.isEmpty()) {
                return@withContext FilterSearchResultsUseCaseOut(emptyList())
            }

            // 1. Identify User & Global Role
            val user = userClient.getById(userId)
            if (user == null) {
                log.warn { "[PermissionFilter] User not found: id=$userId" }
                return@withContext FilterSearchResultsUseCaseOut(emptyList())
            }

            // [Layer 0] System Admin Bypass
            if (user.role == "SYSTEM_ADMIN") {
                log.info { "[PermissionFilter] System Admin bypass for user: ${user.email}" }
                return@withContext FilterSearchResultsUseCaseOut(results)
            }

            log.debug { "[PermissionFilter] Filtering ${results.size} results for user_id=$userId (${user.role})" }

            // Fetch all KB memberships for this user
            val kbMemberships = knowledgeMemberClient.getMembershipsByUserId(userId)
            val membershipMap = kbMemberships.associateBy { it.knowledgeBaseId }

            // Fetch all Path Permissions (Exceptions)
            val pathPermissions = documentPathPermissionRepository.findByUserId(userId)

            val filtered = results.filter { result ->
                val kbId = result.knowledgeBaseId

                // [Layer 1] Knowledge Base Membership Check
                val membership = membershipMap[kbId]
                if (membership == null) {
                    log.trace { "[PermissionFilter] Access Denied: User not a member of KB $kbId" }
                    return@filter false
                }

                // [Layer 1.5] Space Admin Check
                if (membership.role == "ADMIN") {
                    // Space Admin sees EVERYTHING in this KB (ignoring DENY paths)
                    return@filter true
                }

                // [Layer 2] Document Path Check (Member)
                // Default: ALLOW (since member), Exception: DENY
                val docPath = result.path
                if (docPath.isBlank()) return@filter true // Root docs always allowed for members unless specifically denied (implementation detail: root usually safe)

                val matchingPermissions = pathPermissions.filter { perm ->
                    // Check if permission applies to this specific KB (optional safety check if paths are unique across KBs? better to check kbId too if permission has it)
                    (perm.knowledgeBaseId == null || perm.knowledgeBaseId == kbId) &&
                        isPathMatchingOrParent(docPath, perm.documentPath)
                }

                if (matchingPermissions.isEmpty()) {
                    // No specific rules -> Default Allow for Member
                    return@filter true
                }

                val mostSpecific = matchingPermissions.maxByOrNull { it.documentPath.length }!!

                // If most specific rule is DENY -> Block
                if (mostSpecific.permissionLevel == PermissionLevel.DENY) {
                    log.trace {
                        "[PermissionFilter] Access Denied by Path Rule: " +
                            "path=$docPath, matched=${mostSpecific.documentPath}, level=DENY"
                    }
                    return@filter false
                }

                // If most specific rule is READ -> Allow (Explicit Allow overriding a parent Deny, though Deny usually propagates down. Simple logic: Most specific wins)
                return@filter true
            }

            val filteredCount = results.size - filtered.size
            if (filteredCount > 0) {
                log.info { "[PermissionFilter] Filtered out $filteredCount documents (${results.size} -> ${filtered.size})" }
            }

            FilterSearchResultsUseCaseOut(filtered)
        }

    private fun isPathMatchingOrParent(documentPath: String, grantedPath: String): Boolean {
        if (documentPath.isEmpty() || grantedPath.isEmpty()) return false
        if (documentPath == grantedPath) return true
        return documentPath.startsWith("$grantedPath >")
    }
}
