plugins {
    application
    kotlin("plugin.serialization")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

application {
    mainClass.set("org.radarbase.authorizer.Main")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.radarbase:radar-jersey:${Versions.radarJersey}")
    implementation("org.radarbase:radar-jersey-hibernate:${Versions.radarJersey}") {
        runtimeOnly("org.postgresql:postgresql:${Versions.postgresql}")
    }
    implementation("org.radarbase:radar-commons-kotlin:${Versions.radarCommons}")

    implementation("redis.clients:jedis:${Versions.jedis}")

    implementation(enforcedPlatform("io.ktor:ktor-bom:${Versions.ktor}"))
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
