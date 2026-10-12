pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuranReader"
include(":app")

// Optional five-page direct SVG validation app reuses the production renderer.
if (providers.gradleProperty("nativeSvgPrototype").orNull == "true") {
    include(":svg-native-prototype")
}
