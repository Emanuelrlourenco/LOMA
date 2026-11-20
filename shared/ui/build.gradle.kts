// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

kotlin {
    jvm()
    android {
        namespace = "app.loma.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources {
            enable = true
        }
    }
    // Framework estático "LomaShared" que o app iOS (iosApp/) incorpora.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "LomaShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.backhandler)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.loma.ui.generated.resources"
}

roborazzi {
    outputDir.set(file("screenshots"))
}
