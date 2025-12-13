package com.okestro.okchat.email.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@Configuration
@EnableConfigurationProperties(EmailProperties::class)
class EmailConfig {

    /**
     * Dedicated scheduler for blocking email operations (Jakarta Mail)
     * Uses a separate thread pool to avoid blocking the reactive pipeline
     */
    @Bean(name = ["emailScheduler"])
    fun emailScheduler(): Scheduler = Schedulers.newBoundedElastic(
        20, // threadCap
        Integer.MAX_VALUE, // queuedTaskCap
        "email-blocking", // name
        60, // ttlSeconds
        true // daemon
    )
}
