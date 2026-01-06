pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // We keep CI clean: no module-level repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Local AARs (TDLib) live under app/libs
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "PasiflonetMobile"
include(":app")
