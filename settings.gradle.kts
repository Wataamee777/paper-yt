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
        // ↓ これを追加！
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "PaperYt"
include(":app")
