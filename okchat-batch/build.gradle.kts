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
    }
}

dependencies {
    implementation(project(":okchat-lib:okchat-lib-web"))
    implementation(project(":okchat-lib:okchat-lib-persistence"))
    implementation(project(":okchat-domain:okchat-domain-task"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-task")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
