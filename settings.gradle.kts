rootProject.name = "maxzet2KotlinPorject"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("server")
include("client")
include(
    ":server:core",
    ":server:business",
    ":server:persistence-postgres",
    ":server:inference-api",
    ":server:inference-triton",
    ":server:tokenizer",
)
