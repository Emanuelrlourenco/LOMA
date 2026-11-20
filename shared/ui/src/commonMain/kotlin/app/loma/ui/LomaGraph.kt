// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import androidx.compose.runtime.staticCompositionLocalOf
import app.loma.data.SalonRepository
import app.loma.platform.MessagingLauncher
import app.loma.platform.PhotoStore
import app.loma.platform.DeviceAuth
import app.loma.ui.lock.AppLock
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** Dependências do app, criadas uma vez por plataforma e entregues às telas por [LocalLomaGraph]. */
class LomaGraph(
    val repository: SalonRepository,
    val messaging: MessagingLauncher,
    val photos: PhotoStore,
    val clock: Clock,
    val lock: AppLock,
    val deviceAuth: DeviceAuth,
    val zone: () -> TimeZone,
)

val LocalLomaGraph = staticCompositionLocalOf<LomaGraph> { error("LomaGraph não fornecido") }
