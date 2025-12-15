package com.okestro.okchat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OkchatApplication

fun main(args: Array<String>) {
    runApplication<OkchatApplication>(*args)
}
