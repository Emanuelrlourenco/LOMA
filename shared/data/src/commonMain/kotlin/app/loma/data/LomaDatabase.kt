// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [ServiceEntity::class], version = 1, exportSchema = true)
@ConstructedBy(LomaDatabaseConstructor::class)
abstract class LomaDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
}

// O compilador do Room gera as implementações "actual".
@Suppress("KotlinNoActualForExpect")
expect object LomaDatabaseConstructor : RoomDatabaseConstructor<LomaDatabase> {
    override fun initialize(): LomaDatabase
}
