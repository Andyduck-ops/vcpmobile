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

rootProject.name = "naigebao"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:storage")
include(":core:ui")
include(":features:auth")
include(":features:chat")
include(":features:sessions")
include(":features:push")
