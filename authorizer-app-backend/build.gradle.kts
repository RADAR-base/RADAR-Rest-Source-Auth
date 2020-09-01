import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.noarg") version "1.3.61"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.3.61"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.61"
}


application {
    mainClassName = "org.radarbase.authorizer.MainKt"
}

project.extra.apply {
    set("okhttpVersion", "4.2.0")
    set("radarJerseyVersion", "0.2.3")
    set("jacksonVersion", "2.10.2")
    set("slf4jVersion", "1.7.27")
    set("logbackVersion", "1.2.3")
    set("grizzlyVersion", "2.4.4")
    set("jerseyVersion", "2.30")
    set("hibernateVersion", "5.4.10.Final")
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

    implementation("org.radarbase:radar-jersey:${project.extra["radarJerseyVersion"]}")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonVersion"]}")

    implementation("org.slf4j:slf4j-api:${project.extra["slf4jVersion"]}")

    implementation("org.hibernate:hibernate-core:${project.extra["hibernateVersion"]}")
    implementation("org.hibernate:hibernate-c3p0:${project.extra["hibernateVersion"]}")
    implementation("org.liquibase:liquibase-core:3.5.3")

    implementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")

    runtimeOnly("com.h2database:h2:1.4.199")
    runtimeOnly("org.postgresql:postgresql:42.2.5")
    runtimeOnly("ch.qos.logback:logback-classic:${project.extra["logbackVersion"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")

    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:${project.extra["jerseyVersion"]}")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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
