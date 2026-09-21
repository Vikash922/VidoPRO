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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Video Editor"

include(":app")
include(":core:common")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:ui")
include(":core:media")
include(":feature:home")
include(":feature:editor")
include(":feature:timeline")
include(":feature:mediaPicker")
include(":feature:export")
include(":feature:settings")
