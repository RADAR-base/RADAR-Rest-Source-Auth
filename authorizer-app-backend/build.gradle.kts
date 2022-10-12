import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.noarg")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.jetbrains.kotlin.plugin.allopen")
}

application {
    mainClass.set("org.radarbase.authorizer.Main")
    applicationDefaultJvmArgs = listOf(
        "-Djava.security.egd=file:/dev/./urandom",
        "-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager",
    )
}

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    val radarJerseyVersion: String by project
    implementation("org.radarbase:radar-jersey:$radarJerseyVersion")
    implementation("org.radarbase:radar-jersey-hibernate:$radarJerseyVersion")

    val slf4jVersion: String by project
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    val okhttpVersion: String by project
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")

    val jedisVersion: String by project
    implementation("redis.clients:jedis:$jedisVersion")

    val log4j2Version: String by project
    runtimeOnly("org.apache.logging.log4j:log4j-core:$log4j2Version")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    runtimeOnly("org.apache.logging.log4j:log4j-jul:$log4j2Version")

    val junitVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.hamcrest:hamcrest-all:1.3")

    val mockitoKotlinVersion: String by project
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")

    val jerseyVersion: String by project
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:$jerseyVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        apiVersion = "1.7"
        languageVersion = "1.7"
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = FULL
    }
    systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}
