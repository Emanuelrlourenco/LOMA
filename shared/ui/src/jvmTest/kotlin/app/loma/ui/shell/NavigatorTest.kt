// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavigatorTest {

    @Test
    fun empilhaEVolta() {
        val nav = Navigator()
        nav.push(Route.Settings)
        nav.push(Route.Services)
        assertTrue(nav.pop())
        assertEquals(Route.Settings, nav.stack.last())
        assertTrue(nav.pop())
        assertTrue(nav.stack.isEmpty())
        assertFalse(nav.pop())
    }

    @Test
    fun trocaOTopo() {
        val nav = Navigator()
        nav.push(Route.AppointmentDetail(1))
        nav.replaceTop(Route.CompleteAppointment(1))
        assertEquals(listOf<Route>(Route.CompleteAppointment(1)), nav.stack)
    }

    @Test
    fun todasAsRotasVoltamIguaisDoTexto() {
        val rotas = listOf(
            Route.ClientDetail(5), Route.ClientEdit(null), Route.ClientEdit(7), Route.AppointmentDetail(42),
            Route.AppointmentEdit(null, null, null), Route.AppointmentEdit(3, 4, 5), Route.AppointmentEdit(null, 4, null), Route.AppointmentEdit(null, null, null, "2026-11-25"),
            Route.CompleteAppointment(9), Route.Settings, Route.Services, Route.ServiceEdit(null), Route.ServiceEdit(2),
            Route.MessageTemplate,
        )
        rotas.forEach { assertEquals(it, Route.decode(it.encode())) }
    }

    @Test
    fun pilhaSobreviveARecriacao() = runComposeUiTest {
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var visivel by mutableStateOf(true)
        var nav: Navigator? = null
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (visivel) nav = rememberNavigator()
            }
        }
        runOnIdle {
            nav!!.push(Route.Settings)
            nav!!.push(Route.AppointmentDetail(42))
        }
        val salvo = runOnIdle { registry.performSave() }
        runOnIdle { visivel = false }
        runOnIdle {
            registry = SaveableStateRegistry(salvo) { true }
            visivel = true
        }
        runOnIdle { assertEquals(listOf(Route.Settings, Route.AppointmentDetail(42)), nav!!.stack) }
    }
}
