// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import app.loma.data.SettingKeys
import app.loma.platform.MessagingApp
import app.loma.salon.Appointment
import app.loma.salon.AppointmentRules
import app.loma.salon.AppointmentStatus
import app.loma.salon.CancelReason
import app.loma.salon.Client
import app.loma.salon.ConfirmationMessage
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
class AppointmentDetailScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private val coloracao = Service(id = 0, name = "Coloração", durationMinutes = 90, price = Money.ofReais(180), colorArgb = 0)
    private val escova = Service(id = 0, name = "Escova", durationMinutes = 60, price = Money.ofReais(70), colorArgb = 0)

    private fun TestGraph.agendado(transform: (Appointment) -> Appointment = { it }): Long = runBlocking {
        val maria = repository.saveClient(Client(name = "Maria Souza", phoneDigits = "5511987654321", allergies = "amônia", createdAt = saoPaulo(1, 9)))
        repository.save(transform(AppointmentRules.book(maria, listOf(coloracao, escova), saoPaulo(20, 13), SaoPaulo)))
    }

    private fun TestGraph.status(id: Long) = runBlocking { repository.appointment(id).first()!! }

    private fun ready(tag: String) = tag

    @Test
    fun confirmarPeloWhatsAppMontaMensagem() = runComposeUiTest {
        val g = TestGraph()
        val id = g.agendado()
        setContent { TestHost(g.graph) { AppointmentDetailScreen(id) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("confirm_whatsapp").assertExists() }.isSuccess }

        onNodeWithTag("confirm_whatsapp").performClick()

        waitUntil(timeoutMillis = 5_000) { g.messaging.opened.isNotEmpty() }
        val esperado = ConfirmationMessage.render(
            ConfirmationMessage.DEFAULT_TEMPLATE, "Maria Souza", saoPaulo(20, 13), SaoPaulo, LocalDate(2026, 11, 20),
            listOf("Coloração", "Escova"), ConfirmationMessage.WhenLabels("hoje", "amanhã", "e"),
        )
        assertEquals(FakeOpen(MessagingApp.WHATSAPP, "5511987654321", esperado), g.messaging.opened.single().let { FakeOpen(it.app, it.phone, it.text) })

        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("confirmed_yes").assertExists() }.isSuccess }
        onNodeWithTag("confirmed_yes").performScrollTo().performClick()
        waitForData { g.repository.appointment(id).first()!!.status == AppointmentStatus.CONFIRMADO }
    }

    private data class FakeOpen(val app: MessagingApp, val phone: String, val text: String)

    @Test
    fun perguntaQualWhatsAppUmaVez() = runComposeUiTest {
        val g = TestGraph()
        g.messaging.apps = setOf(MessagingApp.WHATSAPP, MessagingApp.WHATSAPP_BUSINESS, MessagingApp.SMS)
        val id = g.agendado()
        setContent { TestHost(g.graph) { AppointmentDetailScreen(id) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("confirm_whatsapp").assertExists() }.isSuccess }

        onNodeWithTag("confirm_whatsapp").performClick()
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("app_WHATSAPP_BUSINESS").assertExists() }.isSuccess }
        onNodeWithTag("app_WHATSAPP_BUSINESS").performClick()

        waitForData { g.repository.setting(SettingKeys.WHATSAPP_APP).first() == "WHATSAPP_BUSINESS" }
        waitUntil(timeoutMillis = 5_000) { g.messaging.opened.size == 1 }
        assertEquals(MessagingApp.WHATSAPP_BUSINESS, g.messaging.opened.single().app)

        onNodeWithTag("confirm_whatsapp").performClick()
        waitUntil(timeoutMillis = 5_000) { g.messaging.opened.size == 2 }
        onNodeWithTag("app_WHATSAPP_BUSINESS").assertDoesNotExist()
    }

    @Test
    fun semWhatsAppOfereceSms() = runComposeUiTest {
        val g = TestGraph()
        g.messaging.apps = setOf(MessagingApp.SMS)
        val id = g.agendado()
        setContent { TestHost(g.graph) { AppointmentDetailScreen(id) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("confirm_whatsapp").assertExists() }.isSuccess }
        onNodeWithTag("confirm_whatsapp").assertTextContains("Confirmar por SMS")
    }

    @Test
    fun desmarcarComMotivoEDesfazer() = runComposeUiTest {
        val g = TestGraph()
        val id = g.agendado()
        setContent { TestHost(g.graph) { AppointmentDetailScreen(id) } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("unschedule").assertExists() }.isSuccess }

        onNodeWithTag("unschedule").performClick()
        onNodeWithTag("reason_CANCELOU").performClick()
        waitForData { g.repository.appointment(id).first()!!.status == AppointmentStatus.DESMARCADO }
        assertEquals(CancelReason.CANCELOU, g.status(id).cancelReason)

        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("undo").assertExists() }.isSuccess }
        onNodeWithTag("undo").performClick()
        waitForData { g.repository.appointment(id).first()!!.status == AppointmentStatus.AGENDADO }
    }

    @Test
    fun acoesPorStatus() = runComposeUiTest {
        val g = TestGraph()
        val agendado = g.agendado()
        val confirmado = g.agendado { AppointmentRules.confirm(it) }
        val concluido = g.agendado { AppointmentRules.complete(it, Money.ofReais(280), saoPaulo(20, 16), "") }
        val desmarcado = g.agendado { AppointmentRules.unschedule(it, CancelReason.CANCELOU) }
        var shown by mutableLongStateOf(agendado)
        setContent { TestHost(g.graph) { androidx.compose.runtime.key(shown) { AppointmentDetailScreen(shown) } } }

        val all = listOf("confirm_whatsapp", "confirmed", "complete", "unschedule", "reopen", "reschedule")
        fun expect(vararg present: String) {
            waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag(present.first()).assertExists() }.isSuccess }
            all.forEach { tag -> if (tag in present) onNodeWithTag(tag).assertExists() else onNodeWithTag(tag).assertDoesNotExist() }
        }
        expect("confirm_whatsapp", "confirmed", "complete", "unschedule")
        shown = confirmado
        expect("complete", "unschedule")
        shown = concluido
        expect("reopen")
        onNodeWithTag("status_pill").assertTextContains("Concluído · R$ 280")
        shown = desmarcado
        expect("reschedule")
        onNodeWithTag("status_pill").assertTextContains("Desmarcado · cancelou")
    }
}
