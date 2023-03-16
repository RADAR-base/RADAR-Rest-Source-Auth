import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1" apply false
    id("com.github.ben-manes.versions") version "0.46.0"
}

allprojects {
    group = "org.radarbase"
    version = "4.2.1-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
//        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        val ktlintVersion: String by project
        version.set(ktlintVersion)
    }

    tasks.register("downloadDependencies") {
        doLast {
            configurations["runtimeClasspath"].files
            configurations["compileClasspath"].files
            println("Downloaded all dependencies")
        }
    }

    tasks.register<Copy>("copyDependencies") {
        from(configurations.runtimeClasspath.map { it.files })
        into("$buildDir/third-party/")
        doLast {
            println("Copied third-party runtime dependencies")
        }
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
    gradleVersion = "8.0.2"
}
