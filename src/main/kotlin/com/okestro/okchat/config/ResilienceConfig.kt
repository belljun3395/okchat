package com.okestro.okchat.config
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.core.registry.EntryAddedEvent
import io.github.resilience4j.core.registry.EntryRemovedEvent
import io.github.resilience4j.core.registry.EntryReplacedEvent
import io.github.resilience4j.core.registry.RegistryEventConsumer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * Resilience4j Circuit Breaker 설정
 *
 * Circuit Breaker 패턴을 사용하여 외부 시스템 장애가 전체 서비스로 전파되지 않도록 방지합니다.
 *
 * Circuit States:
 * - CLOSED (정상): 요청이 정상적으로 통과
 * - OPEN (차단): 실패율/지연율 임계값 초과 시 요청 차단
 * - HALF_OPEN (반개방): 일정 시간 후 테스트 요청 허용
 */
@Configuration
class ResilienceConfig {

    private val log = KotlinLogging.logger {}

    /**
     * 기본 Circuit Breaker 설정
     */
    @Bean
    fun defaultCircuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            // 실패율이 50%를 초과하면 Circuit Open
            .failureRateThreshold(50f)
            // 느린 호출 비율이 50%를 초과하면 Circuit Open
            .slowCallRateThreshold(50f)
            // 2초 이상 걸리는 호출을 "느린 호출"로 간주
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            // Circuit Open 상태에서 30초 대기 후 HALF_OPEN으로 전이
            .waitDurationInOpenState(Duration.ofSeconds(30))
            // HALF_OPEN 상태에서 5개의 테스트 호출 허용
            .permittedNumberOfCallsInHalfOpenState(5)
            // 최근 10개 호출을 sliding window로 사용
            .slidingWindowSize(10)
            // CLOSED에서 최소 5개 호출 후부터 통계 계산
            .minimumNumberOfCalls(5)
            // 자동으로 OPEN → HALF_OPEN 전이
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            // 기록할 예외 타입
            .recordExceptions(
                java.util.concurrent.TimeoutException::class.java,
                java.io.IOException::class.java,
                org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable::class.java
            )
            .build()
    }

    /**
     * Confluence API용 Circuit Breaker 설정
     */
    @Bean
    fun confluenceCircuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(40f) // Confluence는 더 엄격하게 40%
            .slowCallRateThreshold(50f)
            .slowCallDurationThreshold(Duration.ofSeconds(3)) // 3초 이상은 느림
            .waitDurationInOpenState(Duration.ofSeconds(60)) // 1분 대기
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(20) // 더 많은 샘플로 판단
            .minimumNumberOfCalls(10)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()
    }

    /**
     * OpenAI API용 Circuit Breaker 설정
     */
    @Bean
    fun openaiCircuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(30f) // AI는 매우 엄격하게 30%
            .slowCallRateThreshold(40f)
            .slowCallDurationThreshold(Duration.ofSeconds(10)) // AI 응답은 10초까지 허용
            .waitDurationInOpenState(Duration.ofSeconds(120)) // 2분 대기 (비용 절약)
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(15)
            .minimumNumberOfCalls(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()
    }

    /**
     * Circuit Breaker 이벤트 리스너 등록
     *
     * Spring Boot Auto-Configuration에 의해 생성된 CircuitBreakerRegistry에
     * 이벤트 컨슈머를 등록하여 상태 변경 및 에러를 로깅합니다.
     */
    @Bean
    fun circuitBreakerEventConsumer(): RegistryEventConsumer<CircuitBreaker> {
        return object : RegistryEventConsumer<CircuitBreaker> {
            override fun onEntryAddedEvent(entryAddedEvent: EntryAddedEvent<CircuitBreaker>) {
                val circuitBreaker = entryAddedEvent.addedEntry
                circuitBreaker.eventPublisher
                    .onStateTransition { event ->
                        log.info {
                            "[Circuit Breaker] ${event.circuitBreakerName}: " +
                                "${event.stateTransition.fromState} → ${event.stateTransition.toState}"
                        }
                    }
                    .onError { event ->
                        log.warn {
                            "[Circuit Breaker] ${event.circuitBreakerName}: " +
                                "Error recorded - ${event.throwable.message}"
                        }
                    }
                    .onCallNotPermitted { event ->
                        log.warn {
                            "[Circuit Breaker] ${event.circuitBreakerName}: " +
                                "Call not permitted (Circuit is ${circuitBreaker.state})"
                        }
                    }
            }

            override fun onEntryRemovedEvent(entryRemovedEvent: EntryRemovedEvent<CircuitBreaker>) {}
            override fun onEntryReplacedEvent(entryReplacedEvent: EntryReplacedEvent<CircuitBreaker>) {}
        }
    }

    /**
     * Email Circuit Breaker 인스턴스
     */
    @Bean
    fun emailCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker {
        return registry.circuitBreaker("email", "default")
    }
}
