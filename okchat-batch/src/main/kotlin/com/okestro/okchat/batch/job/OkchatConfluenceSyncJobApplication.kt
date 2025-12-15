package com.okestro.okchat.batch.job

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@ConditionalOnProperty(
    name = ["spring.cloud.task.name"],
    havingValue = "confluence-sync",
    matchIfMissing = false
)
@SpringBootApplication(
    scanBasePackages = [
        "com.okestro.okchat.batch",
        "com.okestro.okchat.task",
        "com.okestro.okchat.confluence",
        "com.okestro.okchat.knowledge",
        "com.okestro.okchat.ai",
        "com.okestro.okchat.search",
        "com.okestro.okchat.email",
        "com.okestro.okchat.user"
    ]
)
@EnableJdbcRepositories(basePackages = ["com.okestro.okchat.task.repository"])
@EnableJpaRepositories(basePackages = ["com.okestro.okchat.knowledge.repository", "com.okestro.okchat.user.repository", "com.okestro.okchat.email.repository", "com.okestro.okchat.permission.repository"])
@EntityScan(basePackages = ["com.okestro.okchat.task.entity", "com.okestro.okchat.knowledge.model.entity", "com.okestro.okchat.user.model.entity", "com.okestro.okchat.email.model.entity", "com.okestro.okchat.permission.model.entity"])
class OkchatConfluenceSyncJobApplication

fun main(args: Array<String>) {
    runApplication<OkchatConfluenceSyncJobApplication>(*args) {
        webApplicationType = WebApplicationType.NONE
        setDefaultProperties(
            mapOf(
                "batch.api.enabled" to "false",
                "spring.cloud.task.enabled" to "true",
                "spring.cloud.task.name" to "confluence-sync",
                "task.confluence-sync.enabled" to "true"
            )
        )
    }
}
