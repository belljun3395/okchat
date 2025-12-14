package com.okestro.okchat.batch.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "internal.services")
data class InternalServicesProperties(
    val user: Service = Service(),
    val docs: Service = Service(),
    val ai: Service = Service()
) {
    data class Service(
        val url: String = "http://localhost:8080"
    )
}
