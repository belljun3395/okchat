plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "com.okestro"
version = "0.0.1-SNAPSHOT"
description = "okchat"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.1.0-M2"
extra["springCloudVersion"] = "2025.0.0"

dependencies {
    /** kotlin  &  reactor */
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.projectreactor:reactor-test")

    /** spring boot */
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    /** spring cloud task */
    implementation("org.springframework.cloud:spring-cloud-starter-task")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("com.mysql:mysql-connector-j")

    /** spring ai */
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client-webflux")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-typesense")
    developmentOnly("org.springframework.ai:spring-ai-spring-boot-docker-compose")

    /** logger */
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    /** feign */
    implementation("io.github.openfeign:feign-core:13.5")
    implementation("io.github.openfeign:feign-jackson:13.5")
    implementation("io.github.openfeign:feign-slf4j:13.5")

    /** jakarta mail */
    implementation("org.eclipse.angus:angus-mail:2.0.3")

    /** microsoft oauth2 */
    implementation("com.microsoft.azure:msal4j:1.16.2")

    /** redis */
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    /** test */
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
