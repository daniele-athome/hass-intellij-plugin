rootProject.name = "Home Assistant IntelliJ Plugin"

// TODO remove after IntelliJ Gradle Plugin 2.2.2 has been released
pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
