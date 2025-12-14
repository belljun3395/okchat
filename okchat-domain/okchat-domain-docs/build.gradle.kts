plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("java-library")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    api(project(":okchat-lib:okchat-lib-web"))
    api(project(":okchat-lib:okchat-lib-persistence"))
    api(project(":okchat-lib:okchat-lib-ai"))

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // OpenSearch
    implementation("org.opensearch.client:opensearch-java:2.18.0")
    implementation("org.opensearch.client:opensearch-rest-high-level-client:2.18.0")
    // Fix for AOT: Spring Data Elasticsearch 5.x requires this for hints even if using OpenSearch
    implementation("co.elastic.clients:elasticsearch-java")
    implementation("jakarta.json:jakarta.json-api:2.1.1")
    implementation("org.glassfish:jakarta.json:2.0.1")

    // Retry
    implementation("org.springframework.retry:spring-retry")

    /** feign */
    implementation("io.github.openfeign:feign-core:13.5")
    implementation("io.github.openfeign:feign-jackson:13.5")
    implementation("io.github.openfeign:feign-slf4j:13.5")

    // Spring AI helpers used by docs ingestion/search
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")

    /** tsid */
    implementation("com.github.f4b6a3:tsid-creator:5.2.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
