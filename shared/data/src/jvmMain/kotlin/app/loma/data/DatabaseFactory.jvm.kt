// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

fun lomaDatabaseBuilder(file: File): RoomDatabase.Builder<LomaDatabase> =
    Room.databaseBuilder<LomaDatabase>(name = file.absolutePath)
