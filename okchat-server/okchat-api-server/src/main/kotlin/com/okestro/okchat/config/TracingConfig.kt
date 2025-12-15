package com.okestro.okchat.config

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ThreadLocalAccessor
import io.opentelemetry.context.Context
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Hooks

@Configuration
class TracingConfig {

    @PostConstruct
    fun enableAutomaticContextPropagation() {
        // Enable Reactor automatic context propagation
        Hooks.enableAutomaticContextPropagation()

        // Register OpenTelemetry context for propagation
        ContextRegistry.getInstance()
            .registerThreadLocalAccessor(object : ThreadLocalAccessor<Context> {
                override fun key(): Any = "otel"

                override fun getValue(): Context = Context.current()

                override fun setValue(value: Context) {
                    value.makeCurrent()
                }

                @Deprecated("Deprecated in Java",
                    ReplaceWith("Context.current().makeCurrent()", "io.opentelemetry.context.Context")
                )
                override fun reset() {
                    Context.current().makeCurrent()
                }
            })
    }
}
