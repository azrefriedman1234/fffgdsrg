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
        // בשביל TDLib AAR מקומי
        flatDir { dirs("app/libs") }
    }
}

rootProject.name = "fffgdsrg"
include(":app")
