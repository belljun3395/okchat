package com.okestro.okchat.common.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Aspect to ensure MDC context is preserved across Repository execution
 * This handles cases where underlying JDBC/JPA implementations might clear thread locals
 */
@Aspect
@Component
@Order(1)
class TracePropagationAspect {

    @Around("execution(* com.okestro.okchat..repository..*(..))")
    fun propagateMdc(joinPoint: ProceedingJoinPoint): Any? {
        val contextMap = MDC.getCopyOfContextMap()
        
        try {
            return joinPoint.proceed()
        } finally {
            if (contextMap != null) {
                MDC.setContextMap(contextMap)
            }
        }
    }
}
