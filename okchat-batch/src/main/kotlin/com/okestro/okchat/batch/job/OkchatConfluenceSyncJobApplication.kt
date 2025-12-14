package com.okestro.okchat.batch.job

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.okestro.okchat.batch"])
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
