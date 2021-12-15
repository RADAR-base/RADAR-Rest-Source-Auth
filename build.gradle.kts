import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0" apply false
    id("com.github.ben-manes.versions") version "0.39.0"
}

allprojects {
    group = "org.radarbase"
    version = "3.2.1"

    repositories {
        mavenCentral()
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.wrapper {
    gradleVersion = "7.3.1"
}
