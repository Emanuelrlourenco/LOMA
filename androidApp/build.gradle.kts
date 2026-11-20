// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// APK único: versionCode sempre maior (número da execução do CI), para instalar por cima.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "app.loma.calendar"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.loma.calendar"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = runNumber
        versionName = "0.1.$runNumber"
    }
}

dependencies {
    implementation(project(":shared:ui"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

// O teste do manifest lê o manifest mesclado da variante debug.
tasks.withType<Test>().configureEach {
    dependsOn("processDebugMainManifest")
}
