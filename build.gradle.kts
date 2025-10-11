plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    /** database - hybrid approach */
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") // For domain entities (User, Prompt, Permissions)
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc") // For Spring Cloud Task tables (read-only access)
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2") // For E2E testing

    /** spring cloud task */
    implementation("org.springframework.cloud:spring-cloud-starter-task")
    implementation("org.springframework.integration:spring-integration-core")
    implementation("org.springframework.integration:spring-integration-jdbc")

    /** spring ai */
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client-webflux")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory")
    developmentOnly("org.springframework.ai:spring-ai-spring-boot-docker-compose")

    /** opensearch */
    implementation("org.opensearch.client:spring-data-opensearch:1.5.1")
    implementation("org.opensearch.client:opensearch-rest-high-level-client:2.18.0")
    implementation("org.opensearch.client:opensearch-java:2.18.0")
    implementation("jakarta.json:jakarta.json-api:2.1.1")
    implementation("org.glassfish:jakarta.json:2.0.1")

    /** logger */
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    /** feign */
    implementation("io.github.openfeign:feign-core:13.5")
    implementation("io.github.openfeign:feign-jackson:13.5")
    implementation("io.github.openfeign:feign-slf4j:13.5")

    /** jakarta mail */
    implementation("org.eclipse.angus:angus-mail:2.0.3")

    /** microsoft oauth2 */
    implementation("com.microsoft.azure:msal4j:1.16.2")

    /** jsoup for HTML parsing in emails */
    implementation("org.jsoup:jsoup:1.18.1")

    /** redis */
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    /** swagger/openapi */
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.7.0")
    
    /** security: override vulnerable transitive dependencies */
    implementation("org.apache.commons:commons-lang3:3.18.0")

    /** test */
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
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
