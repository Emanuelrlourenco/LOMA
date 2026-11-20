// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.android

import android.app.Application
import app.loma.data.SalonRepository
import app.loma.data.buildLoma
import app.loma.data.lomaDatabaseBuilder
import app.loma.platform.AndroidMessagingLauncher
import app.loma.platform.AndroidPhotoStore
import app.loma.ui.LomaGraph
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** Cria o banco e as dependências uma vez por processo. */
class LomaApplication : Application() {
    val graph: LomaGraph by lazy {
        LomaGraph(
            repository = SalonRepository(lomaDatabaseBuilder(this).buildLoma(), Clock.System),
            messaging = AndroidMessagingLauncher(this),
            photos = AndroidPhotoStore(this),
            clock = Clock.System,
        ) { TimeZone.currentSystemDefault() }
    }
}
