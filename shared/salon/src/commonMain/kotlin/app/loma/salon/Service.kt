// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

/** Serviço do salão (lista em Ajustes): nome, duração e preço. */
data class Service(
    val id: Long = 0,
    val name: String,
    val durationMinutes: Int,
    val price: Money,
    val colorArgb: Long,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)
