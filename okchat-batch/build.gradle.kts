import org.gradle.api.JavaVersion
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
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
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

springBoot {
    mainClass.set("com.okestro.okchat.batch.OkchatBatchApplicationKt")
}

dependencies {
    implementation(project(":okchat-lib:okchat-lib-web"))
    implementation(project(":okchat-lib:okchat-lib-persistence"))
    implementation(project(":okchat-domain:okchat-domain-task"))
    implementation(project(":okchat-domain:okchat-domain-docs"))
    implementation(project(":okchat-domain:okchat-domain-user"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-task")

    // Database driver
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootJar>("bootJar") {
    mainClass.set("com.okestro.okchat.batch.OkchatBatchApplicationKt")
}

tasks.register<BootJar>("bootJarEmailPolling") {
    group = "build"
    description = "Build Email Polling job executable jar"
    archiveClassifier.set("email-polling")
    mainClass.set("com.okestro.okchat.batch.job.OkchatEmailPollingJobApplicationKt")
    targetJavaVersion.set(JavaVersion.VERSION_21)
    classpath = sourceSets["main"].runtimeClasspath
    from(sourceSets["main"].output)
}

tasks.register<BootJar>("bootJarConfluenceSync") {
    group = "build"
    description = "Build Confluence Sync job executable jar"
    archiveClassifier.set("confluence-sync")
    mainClass.set("com.okestro.okchat.batch.job.OkchatConfluenceSyncJobApplicationKt")
    targetJavaVersion.set(JavaVersion.VERSION_21)
    classpath = sourceSets["main"].runtimeClasspath
    from(sourceSets["main"].output)
}
