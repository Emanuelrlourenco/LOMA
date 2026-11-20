// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.runtime.compositionLocalOf

const val THEME_LIGHT = "claro"
const val THEME_DARK = "escuro"

/** Tema em uso: o que o chamador forçar, senão a escolha salva, senão o tema do celular. */
fun resolveDarkTheme(override: Boolean?, saved: String?, system: Boolean): Boolean = override ?: when (saved) {
    THEME_LIGHT -> false
    THEME_DARK -> true
    else -> system
}

/** Tema atual e a troca pelo botão sol/lua do Início. */
class ThemeToggle(val dark: Boolean, val toggle: () -> Unit)

val LocalThemeToggle = compositionLocalOf<ThemeToggle?> { null }
