package com.okestro.okchat.common.aspect

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.confluence.client.dto.AttachmentListResponse
import com.okestro.okchat.confluence.client.dto.PageListResponse
import com.okestro.okchat.confluence.client.dto.SpaceListResponse
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class MetricAspect(
    private val meterRegistry: MeterRegistry,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(MetricAspect::class.java)

    @Around("execution(* com.okestro.okchat.confluence.client.ConfluenceClient.*(..))")
    fun measureConfluenceClient(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = joinPoint.signature.name
        val sample = Timer.start(meterRegistry)
        var status = "success"
        var exceptionName = "none"

        try {
            val result = joinPoint.proceed()
            recordResponseSize(result)
            return result
        } catch (e: Exception) {
            status = "error"
            exceptionName = e::class.simpleName ?: "unknown"
            throw e
        } finally {
            sample.stop(
                meterRegistry.timer(
                    "confluence.client.request",
                    Tags.of("method", methodName, "status", status, "exception", exceptionName)
                )
            )
        }
    }

    @Around(
        "execution(* org.springframework.ai.vectorstore.VectorStore.add(..)) || " +
            "execution(* org.springframework.ai.vectorstore.VectorStore.delete(..)) || " +
            "execution(* org.springframework.ai.vectorstore.VectorStore.accept(..))"
    )
    fun measureVectorStore(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = joinPoint.signature.name
        val sample = Timer.start(meterRegistry)
        var status = "success"
        var exceptionName = "none"

        try {
            // Measure input size for VectorStore operations (as they are void/boolean mostly)
            val args = joinPoint.args
            recordVectorStoreRequestSize(methodName, args)

            val result = joinPoint.proceed()
            return result
        } catch (e: Exception) {
            status = "error"
            exceptionName = e::class.simpleName ?: "unknown"
            throw e
        } finally {
            sample.stop(
                meterRegistry.timer(
                    "vector.store.operation",
                    Tags.of("operation", methodName, "status", status, "exception", exceptionName)
                )
            )
        }
    }

    private fun recordResponseSize(result: Any?) {
        if (result == null) return

        // 1. Record Item Count
        val itemCount = when (result) {
            is PageListResponse -> result.results.size.toDouble()
            is SpaceListResponse -> result.results.size.toDouble()
            is AttachmentListResponse -> result.results.size.toDouble()
            is Collection<*> -> result.size.toDouble()
            else -> 1.0
        }
        meterRegistry.summary("confluence.client.response.count").record(itemCount)

        // 2. Record Byte Size
        try {
            val byteSize = objectMapper.writeValueAsBytes(result).size.toDouble()
            meterRegistry.summary("confluence.client.response.bytes").record(byteSize)
        } catch (e: Exception) {
            log.warn("Failed to measure response byte size: ${e.message}")
        }
    }

    private fun recordVectorStoreRequestSize(methodName: String, args: Array<Any>) {
        if (args.isEmpty()) return

        // 1. Record Item Count
        val itemCount = when (val arg = args[0]) {
            is Collection<*> -> arg.size.toDouble()
            else -> 1.0
        }
        meterRegistry.summary("vector.store.request.count", Tags.of("operation", methodName)).record(itemCount)

        // 2. Record Byte Size
        try {
            val byteSize = objectMapper.writeValueAsBytes(args[0]).size.toDouble()
            meterRegistry.summary("vector.store.request.bytes", Tags.of("operation", methodName)).record(byteSize)
        } catch (e: Exception) {
            log.warn("Failed to measure request byte size: ${e.message}")
        }
    }
}
