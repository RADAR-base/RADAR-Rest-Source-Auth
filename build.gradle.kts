plugins {
    id("org.radarbase.radar-root-project") version Versions.radarCommons
    id("org.radarbase.radar-dependency-management") version Versions.radarCommons
    id("org.radarbase.radar-kotlin") version Versions.radarCommons apply false
}

radarRootProject {
    projectVersion.set(Versions.project)
    gradleVersion.set(Versions.gradle)
}
