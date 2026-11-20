// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import app.loma.salon.AppointmentRules
import app.loma.salon.AppointmentStatus
import app.loma.salon.Client
import app.loma.salon.Money
import app.loma.salon.Service
import app.loma.ui.SaoPaulo
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.saoPaulo
import app.loma.ui.shell.Navigator
import app.loma.ui.shell.Route
import app.loma.ui.waitForData
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class CompleteAppointmentScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private fun TestGraph.agendado(day: Int = 20): Long = runBlocking {
        val maria = repository.saveClient(Client(name = "Maria Souza", createdAt = saoPaulo(1, 9)))
        val s = listOf(
            Service(name = "Coloração", durationMinutes = 90, price = Money.ofReais(180), colorArgb = 0),
            Service(name = "Escova", durationMinutes = 60, price = Money.ofReais(70), colorArgb = 0),
        )
        repository.save(AppointmentRules.book(maria, s, saoPaulo(day, 13), SaoPaulo))
    }

    private fun ComposeUiTestWait(test: androidx.compose.ui.test.ComposeUiTest) =
        test.waitUntil(timeoutMillis = 5_000) { runCatching { test.onNodeWithTag("final_value").assertTextContains("250") }.isSuccess }

    @Test
    fun concluiComValorAjustado() = runComposeUiTest {
        val g = TestGraph()
        val id = g.agendado()
        val nav = Navigator(listOf(Route.AppointmentDetail(id), Route.CompleteAppointment(id)))
        setContent { TestHost(g.graph, nav) { CompleteAppointmentScreen(id) } }
        ComposeUiTestWait(this)

        onNodeWithTag("final_value").performTextReplacement("280")
        onNodeWithTag("value_diff").assertTextContains("+R$ 30,00 em relação ao esperado")
        onNodeWithTag("complete_note").performTextReplacement("Fez hidratação")
        onNodeWithTag("complete_confirm").performClick()

        waitForData { g.repository.appointment(id).first()!!.status == AppointmentStatus.CONCLUIDO }
        val a = runBlocking { g.repository.appointment(id).first()!! }
        assertEquals(Money.ofReais(280), a.finalValue)
        assertEquals("Fez hidratação", a.note)
        assertEquals(saoPaulo(20, 10), a.completedAt)
        waitUntil(timeoutMillis = 5_000) { nav.stack == listOf<Route>(Route.AppointmentDetail(id)) }
        assertEquals(AppointmentStatus.AGENDADO, nav.pendingUndo?.status)
    }

    @Test
    fun valoresDigitados() = runComposeUiTest {
        val g = TestGraph()
        val id = g.agendado()
        setContent { TestHost(g.graph) { CompleteAppointmentScreen(id) } }
        ComposeUiTestWait(this)

        onNodeWithTag("final_value").performTextReplacement("R$ 1.420,00")
        onNodeWithTag("complete_confirm").assertIsEnabled()
        onNodeWithTag("final_value").performTextReplacement("280,5")
        onNodeWithTag("complete_confirm").assertIsEnabled()
        onNodeWithTag("final_value").performTextReplacement("")
        onNodeWithTag("complete_confirm").assertIsNotEnabled()
        onNodeWithText("Informe o valor final", substring = true).assertExists()
        onNodeWithTag("final_value").performTextReplacement("-10")
        onNodeWithTag("complete_confirm").assertIsNotEnabled()
    }

    @Test
    fun semBaixaDeOntemPagaNoDiaDoAtendimento() = runComposeUiTest {
        val g = TestGraph()
        val id = g.agendado(day = 19)
        setContent { TestHost(g.graph) { CompleteAppointmentScreen(id) } }
        ComposeUiTestWait(this)
        onNodeWithTag("paid_date").assertTextContains("Qui, 19 nov")
    }

    @Test
    fun sobreviveARecriacao() = runComposeUiTest {
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var visivel by mutableStateOf(true)
        val g = TestGraph()
        val id = g.agendado()
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (visivel) TestHost(g.graph) { CompleteAppointmentScreen(id) }
            }
        }
        ComposeUiTestWait(this)
        onNodeWithTag("final_value").performTextReplacement("300")

        val salvo = runOnIdle { registry.performSave() }
        runOnIdle { visivel = false }
        runOnIdle {
            registry = SaveableStateRegistry(salvo) { true }
            visivel = true
        }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("final_value").assertTextContains("300") }.isSuccess }
    }
}
