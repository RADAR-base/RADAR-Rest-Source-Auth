plugins {
    application
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.allopen)
}

application {
    mainClass.set("org.radarbase.authorizer.Main")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(libs.kotlin.reflect)

    implementation(libs.radar.jersey)
    implementation(libs.radar.jersey.hibernate) {
        runtimeOnly(libs.postgresql)
    }
    implementation(libs.radar.commons.kotlin)

    implementation(libs.jedis)

    // Provided by radar-commons
    compileOnly(enforcedPlatform(libs.ktor.bom))
    compileOnly("io.ktor:ktor-client-core")
    compileOnly("io.ktor:ktor-client-auth")
    compileOnly("io.ktor:ktor-client-cio")
    compileOnly("io.ktor:ktor-client-content-negotiation")
    compileOnly("io.ktor:ktor-serialization-kotlinx-json")

    testImplementation(libs.hamcrest)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.jersey.testframework)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
