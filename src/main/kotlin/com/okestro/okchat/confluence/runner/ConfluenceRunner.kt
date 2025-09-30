package com.okestro.okchat.confluence.runner

import com.okestro.okchat.confluence.config.ConfluenceProperties
import com.okestro.okchat.confluence.service.ConfluenceService
import com.okestro.okchat.confluence.service.ContentHierarchy
import com.okestro.okchat.confluence.service.ContentNode
import com.okestro.okchat.confluence.service.ContentType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ConfluenceRunner(
    private val confluenceService: ConfluenceService,
    private val confluenceProperties: ConfluenceProperties
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments?) {
        log.info { "========== Confluence 컨텐츠 조회 시작 ==========" }

        try {
            // Get space key from application arguments or use default
            val spaceKey = args?.getOptionValues("confluence.space")?.firstOrNull() ?: "CBSPPP2411"

            log.info { "스페이스 키: $spaceKey" }
            log.info { "Confluence URL: ${confluenceProperties.baseUrl}" }

            // 1. Get space ID from space key
            log.info { "1. 스페이스 키로 스페이스 ID 조회" }
            val spaceId = confluenceService.getSpaceIdByKey(spaceKey)
            log.info { "✓ 스페이스 ID: $spaceId" }

            // 2. Get all content in hierarchical structure
            log.info { "2. 스페이스 컨텐츠 계층 구조 조회" }
            val hierarchy = confluenceService.getSpaceContentHierarchy(spaceId)
            log.info { "✓ 총 루트 노드 수: ${hierarchy.rootNodes.size}" }
            log.info { "✓ 전체 노드 수: ${hierarchy.getTotalCount()}" }
            log.info { "✓ 폴더 수: ${hierarchy.getCountByType(ContentType.FOLDER)}" }
            log.info { "✓ 페이지 수: ${hierarchy.getCountByType(ContentType.PAGE)}" }
            log.info { "✓ 최대 깊이: ${hierarchy.getMaxDepth()}" }

            // 3. Print the hierarchical structure
            log.info { "3. 스페이스 컨텐츠 계층 구조 출력" }
            log.info { "" }
            log.info { "📚 Confluence 스페이스 구조: $spaceKey" }
            log.info { "─".repeat(60) }
            printContentHierarchy(hierarchy)
            log.info { "─".repeat(60) }
        } catch (e: Exception) {
            log.error(e) { "Confluence 컨텐츠 조회 중 오류 발생: ${e.message}" }
        }

        log.info { "========== Confluence 컨텐츠 조회 완료 ==========" }
    }

    /**
     * Print hierarchical structure
     */
    private fun printContentHierarchy(hierarchy: ContentHierarchy) {
        hierarchy.rootNodes.forEachIndexed { index, node ->
            printNode(node, "", index == hierarchy.rootNodes.size - 1)
        }
    }

    private fun printNode(node: ContentNode, prefix: String, isLast: Boolean) {
        val connector = if (isLast) "└── " else "├── "
        val typeIcon = when (node.type) {
            ContentType.FOLDER -> "📁"
            ContentType.PAGE -> "📄"
        }

        log.info { "$prefix$connector$typeIcon ${node.title} (${node.id})" }

        val childPrefix = prefix + if (isLast) "    " else "│   "
        node.children.forEachIndexed { index, child ->
            printNode(child, childPrefix, index == node.children.size - 1)
        }
    }
}
