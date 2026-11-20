// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// APK único: versionCode sempre maior (número da execução do CI), para instalar por cima.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// Chave de assinatura do APK único (Secrets do GitHub no CI). Sem ela, o release local é assinado com a chave de depuração.
val releaseKeystore = System.getenv("LOMA_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

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

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("LOMA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LOMA_KEY_ALIAS")
                keyPassword = System.getenv("LOMA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 desligado nesta fase.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

// O teste do manifest lê o manifest mesclado da variante debug.
tasks.withType<Test>().configureEach {
    dependsOn("processDebugMainManifest")
    testLogging { events("passed", "failed") }
}
