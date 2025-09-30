package com.okestro.okchat.typense

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class TypesenseRunner(
    private val vectorStore: VectorStore
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments?) {
        log.info { "========== Typesense 튜토리얼 시작 ==========" }

        // 1. 문서 추가 - 프로그래밍 언어와 프레임워크 정보
        log.info { "1. 벡터 저장소에 문서 추가" }
        val documents = listOf(
            Document(
                "Kotlin은 JetBrains에서 개발한 현대적인 프로그래밍 언어입니다. JVM, Android, JavaScript, Native를 지원하며 null 안정성과 간결한 문법이 특징입니다.",
                mapOf("category" to "language", "platform" to "JVM", "year" to "2011")
            ),
            Document(
                "Spring Framework는 Java 기반의 엔터프라이즈 애플리케이션 개발을 위한 오픈소스 프레임워크입니다. 의존성 주입과 AOP를 핵심 기능으로 제공합니다.",
                mapOf("category" to "framework", "language" to "Java", "type" to "backend")
            ),
            Document(
                "Spring AI는 AI 애플리케이션 개발을 단순화하는 Spring 생태계의 프레임워크입니다. LLM, 벡터 DB, 임베딩 등을 Spring 방식으로 통합합니다.",
                mapOf("category" to "framework", "language" to "Java", "type" to "AI")
            ),
            Document(
                "Typesense는 오픈소스 검색 엔진으로 빠른 타이핑 속도에 최적화되어 있습니다. 벡터 검색을 지원하며 Elasticsearch보다 설치와 운영이 간단합니다.",
                mapOf("category" to "database", "type" to "search-engine", "feature" to "vector-search")
            ),
            Document(
                "React는 Facebook에서 개발한 사용자 인터페이스 구축을 위한 JavaScript 라이브러리입니다. 컴포넌트 기반 아키텍처와 가상 DOM을 사용합니다.",
                mapOf("category" to "framework", "language" to "JavaScript", "type" to "frontend")
            ),
            Document(
                "Python은 간결하고 읽기 쉬운 문법을 가진 고수준 프로그래밍 언어입니다. 데이터 과학, 머신러닝, 웹 개발 등 다양한 분야에서 사용됩니다.",
                mapOf("category" to "language", "type" to "general-purpose", "popular" to "data-science")
            ),
            Document(
                "PostgreSQL은 강력한 오픈소스 관계형 데이터베이스 시스템입니다. ACID 트랜잭션을 완벽히 지원하며 확장성과 표준 준수로 유명합니다.",
                mapOf("category" to "database", "type" to "RDBMS", "feature" to "ACID")
            ),
            Document(
                "Docker는 컨테이너 기반 가상화 플랫폼입니다. 애플리케이션과 의존성을 패키징하여 어디서나 동일하게 실행할 수 있습니다.",
                mapOf("category" to "platform", "type" to "containerization", "use" to "DevOps")
            ),
            Document(
                "Kubernetes는 컨테이너 오케스트레이션 플랫폼으로 컨테이너화된 애플리케이션의 배포, 확장, 관리를 자동화합니다.",
                mapOf("category" to "platform", "type" to "orchestration", "use" to "container-management")
            ),
            Document(
                "Redis는 인메모리 데이터 구조 저장소로 데이터베이스, 캐시, 메시지 브로커로 사용됩니다. 매우 빠른 읽기/쓰기 성능이 특징입니다.",
                mapOf("category" to "database", "type" to "in-memory", "use" to "cache")
            )
        )
        vectorStore.add(documents)
        log.info { "✓ ${documents.size}개의 문서가 추가되었습니다." }

        // 2. 다양한 검색 쿼리 테스트
        log.info { "2. 유사도 검색 수행" }

        val queries = listOf(
            "빠른 검색 엔진을 찾고 있어요",
            "데이터베이스 추천해주세요",
            "백엔드 프레임워크가 필요해요",
            "컨테이너 관련 기술을 알고 싶어요"
        )

        queries.forEach { queryText ->
            log.info { "[검색어: $queryText]" }

            val searchResults = vectorStore.similaritySearch(queryText)

            log.info { "검색 결과 TOP 3:" }
            searchResults.take(3).forEachIndexed { index: Int, doc: Document ->
                val distance = doc.metadata["distance"] as? Number
                val similarity = if (distance != null) {
                    val distanceValue = distance.toDouble().coerceIn(0.0, 1.0)
                    String.format("%.4f", 1.0 - distanceValue)
                } else {
                    "N/A"
                }

                log.info { "  [${index + 1}] 유사도: $similarity" }
                log.info { "      내용: ${doc.text?.take(80) ?: ""}..." }
                log.info { "      카테고리: ${doc.metadata["category"]}, 타입: ${doc.metadata["type"]}" }
            }
        }

        log.info { "========== Typesense 튜토리얼 완료 ==========" }
    }
}
