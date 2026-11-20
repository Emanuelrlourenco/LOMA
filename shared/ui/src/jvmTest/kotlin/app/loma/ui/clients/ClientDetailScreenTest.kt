// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.loma.salon.AppointmentRules
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
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class ClientDetailScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private fun TestGraph.maria(allergies: String = "", photo: String? = null): Long = runBlocking {
        repository.saveClient(
            Client(name = "Maria Souza", phoneDigits = "5511987654321", allergies = allergies, photoFile = photo, createdAt = saoPaulo(1, 9)),
        )
    }

    private fun TestGraph.concluido(clientId: Long, day: Int, reais: Long) = runBlocking {
        val s = Service(id = 0, name = "Escova", durationMinutes = 60, price = Money.ofReais(reais), colorArgb = 0)
        repository.save(AppointmentRules.complete(AppointmentRules.book(clientId, listOf(s), saoPaulo(day, 13), SaoPaulo), Money.ofReais(reais), saoPaulo(day, 14), ""))
    }

    @Test
    fun totaisDaFicha() = runComposeUiTest {
        val g = TestGraph()
        val id = g.maria()
        g.concluido(id, 10, 250)
        g.concluido(id, 15, 70)
        setContent { TestHost(g.graph) { ClientDetailScreen(id) } }

        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("stat_visits").assertTextContains("2") }.isSuccess }
        onNodeWithTag("stat_total").assertTextContains("R$ 320")
        onNodeWithTag("stat_ticket").assertTextContains("R$ 160")
        onNodeWithTag("stat_last").assertTextContains("15/11")
    }

    @Test
    fun alergiaEmDestaque() = runComposeUiTest {
        val g = TestGraph()
        val comAlergia = g.maria(allergies = "amônia")
        setContent { TestHost(g.graph) { ClientDetailScreen(comAlergia) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("allergy_band").assertExists() }.isSuccess }
        onNodeWithTag("allergy_band").assertTextContains("Alergia:", substring = true)
    }

    @Test
    fun semAlergiaSemFaixa() = runComposeUiTest {
        val g = TestGraph()
        val id = g.maria()
        setContent { TestHost(g.graph) { ClientDetailScreen(id) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("client_name").assertExists() }.isSuccess }
        onNodeWithTag("allergy_band").assertDoesNotExist()
    }

    @Test
    fun apagarPedeConfirmacao() = runComposeUiTest {
        val g = TestGraph()
        val id = g.maria(photo = "c1.jpg")
        g.photos.files["c1.jpg"] = ByteArray(0)
        val nav = Navigator(listOf(Route.ClientDetail(id)))
        setContent { TestHost(g.graph, nav) { ClientDetailScreen(id) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("client_name").assertExists() }.isSuccess }

        onNodeWithTag("client_menu").performClick()
        onNodeWithTag("menu_delete").performClick()
        onNodeWithTag("delete_cancel").performClick()
        assertTrue(runBlocking { g.repository.client(id).first() } != null)

        onNodeWithTag("client_menu").performClick()
        onNodeWithTag("menu_delete").performClick()
        onNodeWithTag("delete_confirm").performClick()

        waitForData { g.repository.client(id).first() == null }
        waitUntil { nav.stack.isEmpty() }
        assertEquals(listOf("c1.jpg"), g.photos.deleted)
    }
}
