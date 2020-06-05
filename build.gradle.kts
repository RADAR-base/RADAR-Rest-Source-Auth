plugins {
    kotlin("jvm") version "1.3.61" apply false
}

subprojects {
    group = "org.radarbase"
    version = "1.4.0-SNAPSHOT"
}

tasks.wrapper {
    gradleVersion = "6.5"
}
