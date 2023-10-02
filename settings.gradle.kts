rootProject.name = "radar-rest-sources-authorizer"

include(":authorizer-app-backend")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    val kotlinVersion = "1.9.10"
    plugins {
        id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    }
}
