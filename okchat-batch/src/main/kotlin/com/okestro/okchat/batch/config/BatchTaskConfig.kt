package com.okestro.okchat.batch.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.task.configuration.EnableTask
import org.springframework.context.annotation.Configuration

@Configuration
@EnableTask
@ConditionalOnProperty(
    name = ["spring.cloud.task.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class BatchTaskConfig
