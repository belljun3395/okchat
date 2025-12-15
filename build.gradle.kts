plugins {
    kotlin("jvm") apply false
    id("org.jlleitschuh.gradle.ktlint") apply false
}

group = "com.okestro"
version = "0.0.1-SNAPSHOT"
description = "okchat"

subprojects {
    // Apply ktlint only to real Kotlin projects (avoid parent/aggregator projects like `:okchat-domain`).
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }

    pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
        // Ktlint discovers additional source sets (incl. AOT) late in configuration; re-wire after evaluation.
        afterEvaluate {
            tasks.named("ktlintCheck").configure {
                setDependsOn(
                    listOf(
                        "ktlintKotlinScriptCheck",
                        "ktlintMainSourceSetCheck",
                        "ktlintTestSourceSetCheck"
                    )
                )
            }

            tasks.named("ktlintFormat").configure {
                setDependsOn(
                    listOf(
                        "ktlintKotlinScriptFormat",
                        "ktlintMainSourceSetFormat",
                        "ktlintTestSourceSetFormat"
                    )
                )
            }
        }
    }
}

tasks.register("ktlintCheck") {
    group = "verification"
    description = "Runs ktlintCheck for all subprojects."
}

tasks.register("ktlintFormat") {
    group = "formatting"
    description = "Runs ktlintFormat for all subprojects."
}

gradle.projectsEvaluated {
    tasks.named("ktlintCheck").configure {
        dependsOn(
            subprojects
                .filter { it.plugins.hasPlugin("org.jlleitschuh.gradle.ktlint") }
                .map { it.tasks.named("ktlintCheck") }
        )
    }

    tasks.named("ktlintFormat").configure {
        dependsOn(
            subprojects
                .filter { it.plugins.hasPlugin("org.jlleitschuh.gradle.ktlint") }
                .map { it.tasks.named("ktlintFormat") }
        )
    }
}
