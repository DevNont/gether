pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "triptogether"

include(
    ":app",
    ":core:domain",
    ":core:data",
    ":core:ui",
    ":core:testing",
    ":feature:auth",
    ":feature:trip",
    ":feature:plan",
    ":feature:expense",
    ":feature:settlement",
    ":feature:extras",
)
