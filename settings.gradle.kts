// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

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

rootProject.name = "loma"

include(":shared:salon", ":shared:data", ":shared:ui", ":shared:platform", ":androidApp")
