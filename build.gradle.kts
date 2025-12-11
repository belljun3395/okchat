import java.io.File

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    jacoco
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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("com.mysql:mysql-connector-j")

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

    /** kotest */
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("io.kotest:kotest-framework-datatest:5.9.1")
    testImplementation("io.mockk:mockk:1.13.12")

    /** testcontainers */
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testImplementation("com.redis:testcontainers-redis:2.2.2")

    /** monitoring */
    implementation("io.micrometer:micrometer-registry-prometheus")
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
    finalizedBy(tasks.jacocoTestReport)
}

private object JacocoCoverage {
    val generatedClasses = listOf(
        "**/*$\$serializer.class",
        "**/*\$DefaultImpls.class",
        "**/*\$Companion.class",
        "**/*Kt.class",
        "**/*Kt$*.class",
        "**/*\$WhenMappings.class",
        "**/*\$WhenMappings$*.class",
        "**/*\$log$*.class",
        "**/*$*Function*.class",
        "**/*\$lambda$*.class"
    )

    val untrackItems = listOf(
        "**/*Application*.class",
        "**/config/**",
        "**/configuration/**",
        "**/controller/**",
        "**/support/**",
        "**/dto/**",
        "**/model/**",
        "**/entity/**",
        "**/task/**",
        "**/event/**",
        "**/exception/**"
    )

    val externalAdapters = listOf(
        "**/confluence/**",
        "**/client/**",
        "**/repository/**",
        "**/tools/**",
        "**/oauth2/**"
    )

    val excludes: List<String> = generatedClasses + untrackItems + externalAdapters

    private val syntheticSuffixes = listOf(
        "Kt",
        "$\$serializer",
        "\$DefaultImpls",
        "\$Companion"
    )

    private val syntheticFragments = listOf(
        "\$WhenMappings",
        "\$log$",
        "\$lambda$",
        "\$Function"
    )

    fun isSynthetic(file: File): Boolean {
        val name = file.nameWithoutExtension
        return syntheticSuffixes.any { name.endsWith(it) } ||
            syntheticFragments.any { name.contains(it) } ||
            hasNumericSyntheticSuffix(name)
    }

    private fun hasNumericSyntheticSuffix(name: String): Boolean {
        val lastDollar = name.lastIndexOf('$')
        if (lastDollar == -1 || lastDollar == name.lastIndex) {
            return false
        }
        val suffix = name.substring(lastDollar + 1)
        return suffix.firstOrNull()?.isDigit() == true
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    include("com/okestro/**")
                    exclude(JacocoCoverage.excludes)
                }.filter { file -> !JacocoCoverage.isSynthetic(file) }
            }
        )
    )
}

jacoco {
    toolVersion = "0.8.12"
}
