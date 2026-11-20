// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LomaTabTest {

    @Test
    fun voltarDeQualquerAbaLevaAoInicio() {
        assertEquals(LomaTab.Inicio, LomaTab.Agenda.back())
        assertEquals(LomaTab.Inicio, LomaTab.Clientes.back())
        assertEquals(LomaTab.Inicio, LomaTab.Financeiro.back())
    }

    @Test
    fun voltarNoInicioDeixaOSistemaFecharOApp() {
        assertNull(LomaTab.Inicio.back())
    }
}
