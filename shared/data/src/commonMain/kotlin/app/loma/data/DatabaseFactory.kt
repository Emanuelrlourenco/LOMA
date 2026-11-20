// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Nome do arquivo do banco em todas as plataformas. */
const val LOMA_DB_FILE = "loma.db"

/** Aplica o driver SQLite embutido e o despachante de IO, e abre o banco. */
fun RoomDatabase.Builder<LomaDatabase>.buildLoma(): LomaDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
