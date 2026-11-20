// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun lomaDatabaseBuilder(context: Context): RoomDatabase.Builder<LomaDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<LomaDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(LOMA_DB_FILE).absolutePath,
    )
}
