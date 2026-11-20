// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.android

import android.app.Application
import app.loma.data.SalonRepository
import app.loma.data.buildLoma
import app.loma.data.lomaDatabaseBuilder
import app.loma.platform.AndroidMessagingLauncher
import app.loma.platform.AndroidPhotoStore
import app.loma.platform.AndroidDeviceAuth
import app.loma.platform.AndroidKeyValueStore
import app.loma.ui.lock.AppLock
import app.loma.ui.LomaGraph
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** Cria o banco e as dependências uma vez por processo. */
class LomaApplication : Application() {
    /** A MainActivity se entrega aqui para a digital poder abrir o pedido do sistema. */
    val deviceAuth: AndroidDeviceAuth by lazy { AndroidDeviceAuth(this) }

    val graph: LomaGraph by lazy {
        LomaGraph(
            repository = SalonRepository(lomaDatabaseBuilder(this).buildLoma(), Clock.System),
            messaging = AndroidMessagingLauncher(this),
            photos = AndroidPhotoStore(this),
            clock = Clock.System,
            lock = AppLock(AndroidKeyValueStore(this), Clock.System),
            deviceAuth = deviceAuth,
        ) { TimeZone.currentSystemDefault() }
    }
}
