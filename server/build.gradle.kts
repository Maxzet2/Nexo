plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor.io)
    application
}

group = "ru.Maxzet2"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":server:core"))
    implementation(project(":server:business"))
    implementation(project(":server:persistence-postgres"))
    implementation(project(":server:inference-api"))
    implementation(project(":server:inference-triton"))
    implementation(project(":server:tokenizer"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.maxzet2.ApplicationKt")
}

ktor {
    fatJar{ archiveFileName.set("app.jar") }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
}