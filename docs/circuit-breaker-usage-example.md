# Circuit Breaker 사용 가이드

> Resilience4j Circuit Breaker 적용 방법

---

## 설정 확인

### 1. 의존성 (build.gradle.kts)

```kotlin
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")
implementation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
implementation("io.github.resilience4j:resilience4j-kotlin:2.2.0")
```

### 2. Circuit Breaker 설정 (Res ilienceConfig.kt)

3가지 설정 제공:
- `default`: 기본 설정 (50% 실패율, 30초 대기)
- `confluence`: Confluence API용 (40% 실패율, 60초 대기)
- `openai`: OpenAI API용 (30% 실패율, 120초 대기)

---

## 사용 방법

### 예시 1: Confluence Service

```kotlin
package com.okestro.okchat.confluence.service

import com.okestro.okchat.support.executeWithFallback
import com.okestro.okchat.support.executeOrEmptyList
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ConfluenceService(
    private val confluenceClient: ConfluenceClient,
    private val confluenceCircuitBreaker: CircuitBreaker,  // 주입
    private val redisTemplate: ReactiveRedisTemplate<String, Page>
) {

    /**
     * Confluence 페이지 조회 (캐시 Fallback)
     */
    suspend fun getPages(spaceKey: String): List<Page> {
        return confluenceCircuitBreaker.executeWithFallback(
            fallback = { exception ->
                log.warn { "Confluence Circuit Open, using cache for $spaceKey" }
                getCachedPages(spaceKey)
            }
        ) {
            // 실제 API 호출
            val pages = confluenceClient.getPages(spaceKey)
            cachePages(spaceKey, pages)
            pages
        }
    }

    /**
     * Confluence 페이지 검색 (빈 리스트 Fallback)
     */
    suspend fun searchPages(query: String): List<Page> {
        return confluenceCircuitBreaker.executeOrEmptyList {
            confluenceClient.searchPages(query)
        }
    }

    private suspend fun getCachedPages(spaceKey: String): List<Page> {
        return redisTemplate
            .opsForList()
            .range("confluence:pages:$spaceKey", 0, -1)
            .collectList()
            .awaitSingle()
    }

    private suspend fun cachePages(spaceKey: String, pages: List<Page>) {
        val key = "confluence:pages:$spaceKey"
        redisTemplate.delete(key).awaitSingleOrNull()
        if (pages.isNotEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, *pages.toTypedArray()).awaitSingleOrNull()
            redisTemplate.expire(key, Duration.ofHours(1)).awaitSingleOrNull()
        }
    }
}
```

### 예시 2: OpenAI Service

```kotlin
package com.okestro.okchat.ai.service

import com.okestro.okchat.support.executeWithFallback
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.stereotype.Service

@Service
class OpenAiService(
    private val openAiClient: OpenAiClient,
    private val openaiCircuitBreaker: CircuitBreaker  // 주입
) {

    /**
     * Chat 완성 (기본 응답 Fallback)
     */
    suspend fun chat(messages: List<Message>): String {
        return openaiCircuitBreaker.executeWithFallback(
            fallback = { exception ->
                log.warn { "OpenAI Circuit Open, returning fallback message" }
                "죄송합니다. AI 서비스가 일시적으로 사용 불가능합니다. 잠시 후 다시 시도해주세요."
            }
        ) {
            openAiClient.createChatCompletion(messages)
        }
    }

    /**
     * Embedding 생성 (null Fallback)
     */
    suspend fun createEmbedding(text: String): List<Float>? {
        return openaiCircuitBreaker.executeOrNull {
            openAiClient.createEmbedding(text)
        }
    }
}
```

### 예시 3: Email Service

```kotlin
package com.okestro.okchat.email.service

import com.okestro.okchat.support.executeWithFallback
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailProvider: EmailProvider,
    private val emailCircuitBreaker: CircuitBreaker,  // 주입
    private val emailQueueService: EmailQueueService
) {

    /**
     * 이메일 발송 (큐에 저장 Fallback)
     */
    suspend fun sendEmail(to: String, subject: String, body: String) {
        emailCircuitBreaker.executeWithFallback(
            fallback = { exception ->
                log.warn { "Email Circuit Open, queueing email to $to" }
                emailQueueService.enqueue(to, subject, body)
            }
        ) {
            emailProvider.send(to, subject, body)
        }
    }
}
```

---

## Extension Functions 활용

### 1. executeWithFallback
Circuit Open 시 fallback 로직 실행

```kotlin
circuitBreaker.executeWithFallback(
    fallback = { exception ->
        // Circuit Open 시 실행
        getCachedData()
    }
) {
    // 정상 로직
    callExternalApi()
}
```

### 2. executeOrNull
Circuit Open 시 null 반환

```kotlin
val result: Data? = circuitBreaker.executeOrNull {
    callExternalApi()
}
```

### 3. executeOrEmptyList
Circuit Open 시 빈 리스트 반환

```kotlin
val results: List<Data> = circuitBreaker.executeOrEmptyList {
    searchExternalApi(query)
}
```

---

## 모니터링

### Prometheus Metrics

자동으로 수집되는 메트릭:

```promql
# Circuit Breaker 상태
resilience4j_circuitbreaker_state{name="confluence",state="closed"} 1
resilience4j_circuitbreaker_state{name="confluence",state="open"} 0

# 실패율
resilience4j_circuitbreaker_failure_rate{name="confluence"} 0.05

# 느린 호출 비율
resilience4j_circuitbreaker_slow_call_rate{name="confluence"} 0.02

# 호출 통계
resilience4j_circuitbreaker_calls_total{name="confluence",kind="successful"} 1500
resilience4j_circuitbreaker_calls_total{name="confluence",kind="failed"} 10
```

### Grafana Dashboard 쿼리

```promql
# Circuit Breaker 상태 (0: CLOSED, 1: OPEN)
resilience4j_circuitbreaker_state{state="open"}

# 실패율 추이
rate(resilience4j_circuitbreaker_calls_total{kind="failed"}[5m]) /
rate(resilience4j_circuitbreaker_calls_total[5m])

# 느린 호출 비율
resilience4j_circuitbreaker_slow_call_rate
```

---

## Fallback 전략

| 서비스 | Circuit Open 시 Fallback |
|--------|-------------------------|
| **Confluence** | Redis 캐시에서 조회 |
| **OpenAI** | 기본 에러 메시지 반환 |
| **Email** | 재시도 큐에 저장 |
| **Vector Store** | 빈 결과 반환 |

---

## 로그 예시

### Circuit 상태 전이
```
[Circuit Breaker] confluence: CLOSED → OPEN
[Circuit Breaker] confluence: OPEN → HALF_OPEN
[Circuit Breaker] confluence: HALF_OPEN → CLOSED
```

### Fallback 실행
```
[Circuit Breaker] confluence is OPEN, using fallback
Confluence Circuit Open, using cache for SPACE_KEY
```

### 호출 차단
```
[Circuit Breaker] confluence: Call not permitted (Circuit is OPEN)
```

---

## 테스트

### Unit Test 예시

```kotlin
class ConfluenceServiceTest : DescribeSpec({

    val confluenceClient = mockk<ConfluenceClient>()
    val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
    val circuitBreaker = circuitBreakerRegistry.circuitBreaker("test")
    val service = ConfluenceService(confluenceClient, circuitBreaker, ...)

    describe("getPages with Circuit Breaker") {
        it("정상 호출 시 Confluence API 결과 반환") {
            coEvery { confluenceClient.getPages("SPACE") } returns listOf(mockPage)

            val result = service.getPages("SPACE")

            result shouldHaveSize 1
            coVerify { confluenceClient.getPages("SPACE") }
        }

        it("Circuit Open 시 캐시에서 조회") {
            // Circuit을 강제로 OPEN 상태로 만들기
            circuitBreaker.transitionToOpenState()

            val result = service.getPages("SPACE")

            // 캐시에서 조회되어야 함
            result shouldNotBe null
            coVerify(exactly = 0) { confluenceClient.getPages(any()) }
        }
    }
})
```

---

## Best Practices

1. **적절한 설정 선택**
   - 중요한 서비스: 낮은 failureRate (30-40%)
   - 덜 중요한 서비스: 높은 failureRate (50-60%)

2. **의미 있는 Fallback**
   - 캐시 사용 (Confluence, Vector Store)
   - 기본 응답 (AI)
   - 큐 저장 (Email, Notification)

3. **적절한 대기 시간**
   - 빠른 복구: 30-60초
   - 비용 절약: 2-5분

4. **모니터링 필수**
   - Circuit Open 시 알림
   - 실패율 추이 관찰
   - Fallback 사용 빈도 추적

---

**작성일**: 2025-12-12
**작성자**: DevOps Team
