// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.loma.data.SalonRepository
import app.loma.data.buildLoma
import app.loma.data.lomaDatabaseBuilder
import app.loma.platform.IosMessagingLauncher
import app.loma.platform.IosPhotoStore
import app.loma.platform.IosDeviceAuth
import app.loma.platform.IosKeyValueStore
import app.loma.ui.lock.AppLock
import app.loma.ui.shell.LomaApp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

private val iosGraph: LomaGraph by lazy {
    LomaGraph(
        repository = SalonRepository(lomaDatabaseBuilder().buildLoma(), Clock.System),
        messaging = IosMessagingLauncher(),
        photos = IosPhotoStore(),
        clock = Clock.System,
        lock = AppLock(IosKeyValueStore(), Clock.System),
        deviceAuth = IosDeviceAuth(),
    ) { TimeZone.currentSystemDefault() }
}

// Bloqueio por tempo: o app foi para o segundo plano e voltou (os observadores ficam com a central).
private val lifecycleObservers: List<Any> by lazy {
    val center = NSNotificationCenter.defaultCenter
    listOf(
        center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ -> iosGraph.lock.onBackground() },
        center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, NSOperationQueue.mainQueue) { _ -> iosGraph.lock.onForeground() },
    )
}

/** Ponto de entrada do iOS: o app SwiftUI hospeda este controlador (iosApp/iosApp/ContentView.swift). */
fun MainViewController(): UIViewController {
    lifecycleObservers
    lateinit var controller: UIViewController
    // A barra de status segue o tema do app (o botão sol/lua pode diferir do iPhone).
    controller = ComposeUIViewController {
        LomaApp(iosGraph, onDarkThemeChange = { dark ->
            controller.overrideUserInterfaceStyle = if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
        })
    }
    return controller
}
