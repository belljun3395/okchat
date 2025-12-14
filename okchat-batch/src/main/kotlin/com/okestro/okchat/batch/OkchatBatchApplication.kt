package com.okestro.okchat.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.okestro.okchat"])
class OkchatBatchApplication

fun main(args: Array<String>) {
    runApplication<OkchatBatchApplication>(*args) {
        setDefaultProperties(
            mapOf(
                "batch.api.enabled" to "true"
            )
        )
    }
}
