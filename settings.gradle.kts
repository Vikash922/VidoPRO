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

// Keep builds usable in offline/air-gapped environments. The project does not declare a
// Java toolchain, so Foojay's automatic toolchain-provisioning plugin is unnecessary.
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
