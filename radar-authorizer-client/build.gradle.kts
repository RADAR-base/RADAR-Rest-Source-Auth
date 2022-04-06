plugins {
    kotlin("jvm")
}

dependencies {
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.0.0")

    val radarSchemasVersion: String by project
    implementation("org.radarbase:radar-schemas-commons:$radarSchemasVersion")

    val radarAuthVersion: String by project
    implementation("org.radarbase:oauth-client-util:$radarAuthVersion")

    val okhttpVersion: String by project
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
}

