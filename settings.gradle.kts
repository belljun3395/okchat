rootProject.name = "okchat"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "1.9.25"
        kotlin("plugin.spring") version "1.9.25"
        kotlin("plugin.jpa") version "1.9.25"
        id("org.springframework.boot") version "3.5.6"
        id("io.spring.dependency-management") version "1.1.7"
        id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
        id("org.graalvm.buildtools.native") version "0.10.3"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":okchat-lib:okchat-lib-web")
include(":okchat-lib:okchat-lib-persistence")
include(":okchat-lib:okchat-lib-ai")

// Servers
include(":okchat-server:okchat-api-server")
include(":okchat-server:okchat-batch-server")
project(":okchat-server:okchat-batch-server").projectDir = file("okchat-batch")

// Domains
include(":okchat-domain:okchat-domain-user")
include(":okchat-domain:okchat-domain-docs")
include(":okchat-domain:okchat-domain-ai")
include(":okchat-domain:okchat-domain-task")
