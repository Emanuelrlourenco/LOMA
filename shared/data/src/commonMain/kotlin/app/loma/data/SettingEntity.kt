// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Ajuste simples chave → valor (perfil, modelo da mensagem, app do WhatsApp). */
@Entity(tableName = "settings")
data class SettingEntity(@PrimaryKey val key: String, val value: String)
