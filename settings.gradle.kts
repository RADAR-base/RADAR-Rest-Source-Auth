rootProject.name = "radar-rest-sources-authorizer"
include(":authorizer-app-backend")

pluginManagement {
    val kotlin = "1.9.10"
    plugins {
        id("org.jetbrains.kotlin.plugin.noarg") version kotlin
        id("org.jetbrains.kotlin.plugin.jpa") version kotlin
        id("org.jetbrains.kotlin.plugin.allopen") version kotlin
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
