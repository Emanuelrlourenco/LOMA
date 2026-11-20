// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.loma.data.SalonRepository
import app.loma.data.buildLoma
import app.loma.data.lomaDatabaseBuilder
import app.loma.platform.IosMessagingLauncher
import app.loma.platform.IosPhotoStore
import app.loma.ui.shell.LomaApp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import platform.UIKit.UIViewController

private val iosGraph: LomaGraph by lazy {
    LomaGraph(
        repository = SalonRepository(lomaDatabaseBuilder().buildLoma(), Clock.System),
        messaging = IosMessagingLauncher(),
        photos = IosPhotoStore(),
        clock = Clock.System,
    ) { TimeZone.currentSystemDefault() }
}

/** Ponto de entrada do iOS: o app SwiftUI hospeda este controlador (iosApp/iosApp/ContentView.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController { LomaApp(iosGraph) }
