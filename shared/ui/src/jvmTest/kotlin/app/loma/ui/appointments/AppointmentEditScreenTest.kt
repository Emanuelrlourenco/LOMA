// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import app.loma.ui.waitForData
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalTestApi::class)
class AppointmentEditScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private class Dados(val g: TestGraph, val maria: Long, val coloracao: Service, val escova: Service)

    private fun dados(): Dados {
        val g = TestGraph()
        return runBlocking {
            val c = Service(name = "Coloração", durationMinutes = 90, price = Money.ofReais(180), colorArgb = 0, sortOrder = 0)
            val e = Service(name = "Escova", durationMinutes = 60, price = Money.ofReais(70), colorArgb = 0, sortOrder = 1)
            val cs = c.copy(id = g.repository.saveService(c))
            val es = e.copy(id = g.repository.saveService(e))
            val maria = g.repository.saveClient(Client(name = "Maria Souza", phoneDigits = "5511987654321", createdAt = saoPaulo(1, 9)))
            Dados(g, maria, cs, es)
        }
    }

    private fun todos(g: TestGraph) = runBlocking {
        g.repository.appointmentsBetween(LocalDate(2026, 11, 1), LocalDate(2027, 2, 1), SaoPaulo).first()
    }

    @Test
    fun agendarSomaEMostra() = runComposeUiTest {
        val d = dados()
        setContent { TestHost(d.g.graph) { AppointmentEditScreen(id = null, clientId = d.maria, rescheduleOf = null, day = "2026-11-20") } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("svc_${d.escova.id}").assertExists() }.isSuccess }

        onNodeWithTag("svc_${d.coloracao.id}").performClick()
        onNodeWithTag("svc_${d.escova.id}").performClick()
        onNodeWithTag("start_time").performTextReplacement("13:00")

        onNodeWithTag("time_range").assertTextContains("13:00 – 15:30")
        onNodeWithTag("appt_value").assertTextContains("250")
        onNodeWithTag("save").performScrollTo().performClick()

        waitForData { todos(d.g).size == 1 }
        val a = todos(d.g).single()
        assertEquals(AppointmentStatus.AGENDADO, a.status)
        assertEquals(Money.ofReais(250), a.expected)
        assertEquals(saoPaulo(20, 13), a.start)
        assertEquals(d.maria, a.clientId)
    }

    @Test
    fun valorEditavel() = runComposeUiTest {
        val d = dados()
        setContent { TestHost(d.g.graph) { AppointmentEditScreen(id = null, clientId = d.maria, rescheduleOf = null, day = "2026-11-20") } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("svc_${d.coloracao.id}").assertExists() }.isSuccess }
        onNodeWithTag("svc_${d.coloracao.id}").performClick()
        onNodeWithTag("start_time").performTextReplacement("13:00")
        onNodeWithTag("appt_value").performTextReplacement("230")
        onNodeWithTag("save").performScrollTo().performClick()
        waitForData { todos(d.g).size == 1 }
        assertEquals(Money.ofReais(230), todos(d.g).single().expected)
    }

    @Test
    fun avisaConflitoMasSalva() = runComposeUiTest {
        val d = dados()
        runBlocking { d.g.repository.save(AppointmentRules.book(d.maria, listOf(d.escova), saoPaulo(20, 14), SaoPaulo)) }
        setContent { TestHost(d.g.graph) { AppointmentEditScreen(id = null, clientId = d.maria, rescheduleOf = null, day = "2026-11-20") } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("svc_${d.coloracao.id}").assertExists() }.isSuccess }
        onNodeWithTag("svc_${d.coloracao.id}").performClick()
        onNodeWithTag("start_time").performTextReplacement("13:00")

        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("conflict_warning").assertExists() }.isSuccess }
        onNodeWithTag("conflict_warning").assertTextContains("14:00 Maria Souza", substring = true)
        onNodeWithTag("save").performScrollTo().performClick()
        waitForData { todos(d.g).size == 2 }
    }

    @Test
    fun repetir4Semanas() = runComposeUiTest {
        val d = dados()
        setContent { TestHost(d.g.graph) { AppointmentEditScreen(id = null, clientId = d.maria, rescheduleOf = null, day = "2026-11-20") } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("svc_${d.escova.id}").assertExists() }.isSuccess }
        onNodeWithTag("svc_${d.escova.id}").performClick()
        onNodeWithTag("start_time").performTextReplacement("13:00")
        onNodeWithTag("repeat").performScrollTo().performClick()
        onNodeWithTag("repeat_times").performScrollTo().performTextReplacement("4")
        onNodeWithTag("save").performScrollTo().performClick()

        waitForData { todos(d.g).size == 4 }
        assertEquals(listOf(20, 27, 4, 11), todos(d.g).map { it.start.toString().let { s -> LocalDate.parse(s.substring(0, 10)).day } })
    }

    @Test
    fun semClienteOuSemServicoNaoSalva() = runComposeUiTest {
        val d = dados()
        setContent { TestHost(d.g.graph) { AppointmentEditScreen(id = null, clientId = null, rescheduleOf = null, day = "2026-11-20") } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("svc_${d.escova.id}").assertExists() }.isSuccess }
        onNodeWithTag("svc_${d.escova.id}").performClick()
        onNodeWithTag("start_time").performTextReplacement("13:00")
        onNodeWithTag("save").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun clienteSemServicoNaoSalva() = runComposeUiTest {
        val d = dados()
        setContent { TestHost(d.g.graph) { AppointmentEditScreen(id = null, clientId = d.maria, rescheduleOf = null, day = "2026-11-20") } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("svc_${d.escova.id}").assertExists() }.isSuccess }
        onNodeWithTag("start_time").performTextReplacement("13:00")
        onNodeWithTag("save").performScrollTo().assertIsNotEnabled()
    }
}
