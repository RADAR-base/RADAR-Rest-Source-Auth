rootProject.name = "radar-rest-sources-authorizer"

include(":authorizer-app-backend")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    val kotlinVersion = "1.9.10"
    plugins {
        kotlin("plugin.serialization") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
    }
}
