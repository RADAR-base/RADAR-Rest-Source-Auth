plugins {
    kotlin("jvm") version "1.4.0" apply false
}

subprojects {
    group = "org.radarbase"
    version = "2.0.0-SNAPSHOT"
}

tasks.wrapper {
    gradleVersion = "6.6.1"
}
