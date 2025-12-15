package com.okestro.okchat.support

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction

private val log = KotlinLogging.logger {}

/**
 * Circuit Breaker Extension Functions for Kotlin Coroutines
 */

/**
 * Circuit Breaker를 통해 suspend 함수 실행 (fallback 포함)
 *
 * @param fallback Circuit이 OPEN 상태일 때 실행할 대체 로직
 * @param block 실행할 비즈니스 로직
 */
suspend fun <T> CircuitBreaker.executeWithFallback(
    fallback: suspend (CallNotPermittedException) -> T,
    block: suspend () -> T
): T {
    return try {
        this.executeSuspendFunction(block)
    } catch (e: CallNotPermittedException) {
        log.warn {
            "[Circuit Breaker] ${this.name} is ${this.state}, using fallback"
        }
        fallback(e)
    }
}

/**
 * Circuit Breaker를 통해 suspend 함수 실행 (null 반환 fallback)
 *
 * Circuit이 OPEN 상태이거나 에러 발생 시 null 반환
 */
suspend fun <T> CircuitBreaker.executeOrNull(
    block: suspend () -> T
): T? {
    return try {
        this.executeSuspendFunction(block)
    } catch (e: CallNotPermittedException) {
        log.warn {
            "[Circuit Breaker] ${this.name} is ${this.state}, returning null"
        }
        null
    } catch (e: Exception) {
        log.error(e) {
            "[Circuit Breaker] ${this.name} execution failed, returning null"
        }
        null
    }
}

/**
 * Circuit Breaker를 통해 suspend 함수 실행 (빈 리스트 반환 fallback)
 *
 * Circuit이 OPEN 상태이거나 에러 발생 시 빈 리스트 반환
 */
suspend fun <T> CircuitBreaker.executeOrEmptyList(
    block: suspend () -> List<T>
): List<T> {
    return try {
        this.executeSuspendFunction(block)
    } catch (e: CallNotPermittedException) {
        log.warn {
            "[Circuit Breaker] ${this.name} is ${this.state}, returning empty list"
        }
        emptyList()
    } catch (e: Exception) {
        log.error(e) {
            "[Circuit Breaker] ${this.name} execution failed, returning empty list"
        }
        emptyList()
    }
}

/**
 * Circuit Breaker 상태 확인
 */
fun CircuitBreaker.isOpen(): Boolean = this.state == CircuitBreaker.State.OPEN

fun CircuitBreaker.isClosed(): Boolean = this.state == CircuitBreaker.State.CLOSED

fun CircuitBreaker.isHalfOpen(): Boolean = this.state == CircuitBreaker.State.HALF_OPEN

/**
 * Circuit Breaker 메트릭 정보 출력
 */
fun CircuitBreaker.logMetrics() {
    val metrics = this.metrics
    log.info {
        """
        [Circuit Breaker] ${this.name} Metrics:
        - State: ${this.state}
        - Failure Rate: ${metrics.failureRate}%
        - Slow Call Rate: ${metrics.slowCallRate}%
        - Number of Buffered Calls: ${metrics.numberOfBufferedCalls}
        - Number of Failed Calls: ${metrics.numberOfFailedCalls}
        - Number of Slow Calls: ${metrics.numberOfSlowCalls}
        - Number of Successful Calls: ${metrics.numberOfSuccessfulCalls}
        """.trimIndent()
    }
}
