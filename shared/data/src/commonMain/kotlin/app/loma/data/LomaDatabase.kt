// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        ServiceEntity::class,
        ClientEntity::class,
        AppointmentEntity::class,
        AppointmentServiceEntity::class,
        SettingEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(LomaDatabaseConstructor::class)
abstract class LomaDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
    abstract fun clientDao(): ClientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun settingDao(): SettingDao
}

// O compilador do Room gera as implementações "actual".
@Suppress("KotlinNoActualForExpect")
expect object LomaDatabaseConstructor : RoomDatabaseConstructor<LomaDatabase> {
    override fun initialize(): LomaDatabase
}
