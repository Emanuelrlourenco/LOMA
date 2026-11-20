// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.loma.ui.shell.LomaApp
import platform.UIKit.UIViewController

/** Ponto de entrada do iOS: o app SwiftUI hospeda este controlador (iosApp/iosApp/ContentView.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController { LomaApp() }
