import org.radarbase.gradle.plugin.radarKotlin

plugins {
    application
    id("org.radarbase.radar-kotlin")
    kotlin("plugin.serialization") version Versions.kotlin
    kotlin("plugin.noarg") version Versions.kotlin
    kotlin("plugin.jpa") version Versions.kotlin
    kotlin("plugin.allopen") version Versions.kotlin
}

application {
    mainClass.set("org.radarbase.authorizer.Main")
}

dependencies {
    implementation(kotlin("reflect"))

    implementation("org.radarbase:radar-jersey:${Versions.radarJersey}")
    implementation("org.radarbase:radar-jersey-hibernate:${Versions.radarJersey}") {
        runtimeOnly("org.postgresql:postgresql:${Versions.postgresql}")
    }
    implementation("org.radarbase:radar-commons-kotlin:${Versions.radarCommons}")

    implementation("redis.clients:jedis:${Versions.jedis}")

    implementation(enforcedPlatform("io.ktor:ktor-bom"))
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    testImplementation("org.hamcrest:hamcrest:${Versions.hamcrest}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:${Versions.jersey}")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

radarKotlin {
    javaVersion.set(Versions.java)
    log4j2Version.set(Versions.log4j2)
    sentryEnabled.set(true)
}
