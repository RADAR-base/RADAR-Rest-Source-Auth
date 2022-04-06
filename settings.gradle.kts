rootProject.name = "radar-rest-sources-authorizer"
include(":authorizer-app-backend")
include(":radar-authorizer-client")

pluginManagement {
    val kotlinVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    }
}
