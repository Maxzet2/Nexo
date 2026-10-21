plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    api(project(":server:core"))
    implementation(project(":server:inference-api"))
    implementation(project(":server:tokenizer"))
    // ChatService already manages Exposed transactions; preserve this coupling.
    implementation(libs.exposed.jdbc)
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
