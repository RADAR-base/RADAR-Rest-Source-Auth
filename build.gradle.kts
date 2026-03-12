import org.radarbase.gradle.plugin.radarKotlin

plugins {
    alias(libs.plugins.radar.root.project)
    alias(libs.plugins.radar.dependency.management)
    alias(libs.plugins.radar.kotlin) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.noarg) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.kotlin.allopen) apply false
}

radarRootProject {
    projectVersion.set(libs.versions.project)
    gradleVersion.set(libs.versions.gradle)
}

subprojects {
    apply(plugin = "org.radarbase.radar-kotlin")

    radarKotlin {
        log4j2Version.set(rootProject.libs.versions.log4j2)
        sentryEnabled.set(true)
    }
}
