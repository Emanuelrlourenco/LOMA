// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeChoiceTest {
    @Test
    fun forcadoPeloChamadorVence() {
        assertEquals(true, resolveDarkTheme(override = true, saved = "claro", system = false))
        assertEquals(false, resolveDarkTheme(override = false, saved = "escuro", system = true))
    }

    @Test
    fun escolhaSalvaVenceOSistema() {
        assertEquals(false, resolveDarkTheme(override = null, saved = "claro", system = true))
        assertEquals(true, resolveDarkTheme(override = null, saved = "escuro", system = false))
    }

    @Test
    fun semEscolhaSegueOSistema() {
        assertEquals(true, resolveDarkTheme(override = null, saved = null, system = true))
        assertEquals(false, resolveDarkTheme(override = null, saved = "", system = false))
    }
}
