// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MigrationTest {

    @Test
    fun migra1para2SemPerderServicos() = runTest {
        val dbPath = Files.createTempDirectory("loma-mig").resolve("loma.db")
        val helper = MigrationTestHelper(
            schemaDirectoryPath = Paths.get("schemas"),
            databasePath = dbPath,
            driver = BundledSQLiteDriver(),
            databaseClass = LomaDatabase::class,
        )
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO services (name, durationMinutes, priceCents, colorArgb, active, sortOrder) VALUES ('Escova', 60, 7000, 0, 1, 0)")
            close()
        }

        val conn = helper.runMigrationsAndValidate(2)
        conn.prepare("SELECT name, priceCents FROM services").use {
            assertTrue(it.step())
            assertEquals("Escova", it.getText(0))
            assertEquals(7000L, it.getLong(1))
        }
        val tabelas = mutableListOf<String>()
        conn.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").use { while (it.step()) tabelas += it.getText(0) }
        assertTrue(tabelas.containsAll(listOf("clients", "appointments", "appointment_services", "settings")), "tabelas: $tabelas")
        conn.close()
    }
}
