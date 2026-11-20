// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Serviço do salão: nome, duração e preço (em centavos). */
@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationMinutes: Int,
    val priceCents: Long,
    val colorArgb: Long,
    val active: Boolean = true,
    val sortOrder: Int,
)
