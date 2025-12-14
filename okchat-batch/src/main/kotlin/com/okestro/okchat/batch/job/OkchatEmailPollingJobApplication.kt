package com.okestro.okchat.batch.job

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.okestro.okchat.batch"])
class OkchatEmailPollingJobApplication

fun main(args: Array<String>) {
    runApplication<OkchatEmailPollingJobApplication>(*args) {
        webApplicationType = WebApplicationType.NONE
        setDefaultProperties(
            mapOf(
                "batch.api.enabled" to "false",
                "spring.cloud.task.enabled" to "true",
                "spring.cloud.task.name" to "email-polling",
                "task.email-polling.enabled" to "true"
            )
        )
    }
}
