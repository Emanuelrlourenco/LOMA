// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.time.Instant

/** Aniversário: dia e mês; o ano é opcional. */
data class Birthday(val day: Int, val month: Int, val year: Int? = null)

/** Ficha da cliente (design §7.4). Telefone só com dígitos e DDI. */
data class Client(
    val id: Long = 0,
    val name: String,
    val phoneDigits: String? = null,
    val birthday: Birthday? = null,
    val photoFile: String? = null,
    val allergies: String = "",
    val preferences: String = "",
    val notes: String = "",
    val createdAt: Instant,
    val archived: Boolean = false,
)
