package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import kotlin.coroutines.Continuation

private val log = KotlinLogging.logger {}

/**
 * Aspect for monitoring performance of service methods
 * Logs slow operations and provides performance metrics
 */
@Aspect
@Component
class PerformanceMonitoringAspect {

    companion object {
        private const val SLOW_OPERATION_THRESHOLD_MS = 1000L
        private const val VERY_SLOW_OPERATION_THRESHOLD_MS = 5000L
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    fun serviceClassMethods() {}

    @Pointcut("@within(org.springframework.stereotype.Repository)")
    fun repositoryClassMethods() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    fun webEndpointMethods() {}

    @Around("serviceClassMethods() || repositoryClassMethods() || webEndpointMethods()")
    fun monitorPerformance(joinPoint: ProceedingJoinPoint): Any? {
        val stopWatch = StopWatch()
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        
        // Skip monitoring for suspend functions (handled differently in coroutines)
        val isSuspendFunction = joinPoint.args.lastOrNull()?.let { 
            it is Continuation<*> 
        } ?: false
        
        if (isSuspendFunction) {
            return joinPoint.proceed()
        }

        stopWatch.start()
        
        return try {
            val result = joinPoint.proceed()
            
            stopWatch.stop()
            val executionTime = stopWatch.totalTimeMillis
            
            when {
                executionTime > VERY_SLOW_OPERATION_THRESHOLD_MS -> {
                    log.error { "VERY SLOW OPERATION: $methodName took ${executionTime}ms" }
                }
                executionTime > SLOW_OPERATION_THRESHOLD_MS -> {
                    log.warn { "SLOW OPERATION: $methodName took ${executionTime}ms" }
                }
                executionTime > 500 -> {
                    log.info { "Performance: $methodName took ${executionTime}ms" }
                }
                else -> {
                    log.debug { "Performance: $methodName took ${executionTime}ms" }
                }
            }
            
            result
        } catch (e: Exception) {
            stopWatch.stop()
            log.error(e) { "Error in $methodName after ${stopWatch.totalTimeMillis}ms" }
            throw e
        }
    }
}