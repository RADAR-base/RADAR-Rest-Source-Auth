import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.noarg")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.jetbrains.kotlin.plugin.allopen")
}


application {
    mainClassName = "org.radarbase.authorizer.MainKt"
}

project.extra.apply {
    set("okhttpVersion", "4.8.1")
    set("radarJerseyVersion", "0.3.0-SNAPSHOT")
    set("jacksonVersion", "2.11.2")
    set("slf4jVersion", "1.7.30")
    set("logbackVersion", "1.2.3")
    set("jerseyVersion", "2.31")
    set("liquibaseVersion", "3.10.2")
    set("junitVersion", "5.6.2")
    set("mockitoKotlinVersion", "2.2.0")
    set("githubRepoName", "RADAR-base/RADAR-Rest-Source-Auth")
    set("githubUrl", "https://github.com/RADAR-base/RADAR-Rest-Source-Auth.git")
    set("issueUrl", "https://github.com/RADAR-base/RADAR-Rest-Source-Auth/issues")
    set("website", "http://radar-base.org")
    set("description", "RADAR Rest Source Authorizer handles authorization for data access from third party APIs for wearable devices or other connected sources.")
}


repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://dl.bintray.com/radar-base/org.radarbase")
    maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
    maven(url = "https://repo.thehyve.nl/content/repositories/snapshots")
    maven(url = "https://oss.jfrog.org/artifactory/libs-snapshot/")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.radarbase:radar-jersey:${project.extra["radarJerseyVersion"]}")
    implementation("org.radarbase:radar-jersey-hibernate:${project.extra["radarJerseyVersion"]}")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonVersion"]}")

    implementation("org.slf4j:slf4j-api:${project.extra["slf4jVersion"]}")

    implementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")

    runtimeOnly("ch.qos.logback:logback-classic:${project.extra["logbackVersion"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${project.extra["junitVersion"]}")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${project.extra["mockitoKotlinVersion"]}")

    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:${project.extra["jerseyVersion"]}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.4"
        languageVersion = "1.4"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}

tasks.register("downloadDependencies") {
    configurations["runtimeClasspath"].files
    configurations["compileClasspath"].files

    doLast {
        println("Downloaded all dependencies")
    }
}
