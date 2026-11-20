// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Ficha da cliente (design §7.4). */
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneDigits: String?,
    val birthdayDay: Int?,
    val birthdayMonth: Int?,
    val birthdayYear: Int?,
    val photoFile: String?,
    val allergies: String,
    val preferences: String,
    val notes: String,
    val createdAtMs: Long,
    val archived: Boolean,
)
