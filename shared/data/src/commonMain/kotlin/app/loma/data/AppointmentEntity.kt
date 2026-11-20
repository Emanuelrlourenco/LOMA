// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Atendimento. Tempos em epoch ms; status e motivo como texto. */
@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("clientId"), Index("startMs"), Index("completedAtMs")],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long?,
    val startMs: Long,
    val endMs: Long,
    val zoneId: String,
    val status: String,
    val cancelReason: String?,
    val expectedCents: Long,
    val finalCents: Long?,
    val completedAtMs: Long?,
    val note: String,
)
