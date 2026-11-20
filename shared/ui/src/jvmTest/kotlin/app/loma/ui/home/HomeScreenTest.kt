// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.loma.data.SettingKeys
import app.loma.salon.AppointmentRules
import app.loma.salon.Birthday
import app.loma.salon.CancelReason
import app.loma.salon.Client
import app.loma.salon.Money
import app.loma.salon.Service
import app.loma.ui.SaoPaulo
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.saoPaulo
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private fun svc(reais: Long, min: Int = 60) = Service(name = "Serviço", durationMinutes = min, price = Money.ofReais(reais), colorArgb = 0)

    private fun TestGraph.cliente(nome: String, birthday: Birthday? = null) = runBlocking {
        repository.saveClient(Client(name = nome, phoneDigits = "5511987654321", birthday = birthday, createdAt = saoPaulo(1, 9)))
    }

    private fun TestGraph.agenda(cliente: Long, start: Instant, reais: Long, min: Int = 60) = runBlocking {
        repository.save(AppointmentRules.book(cliente, listOf(svc(reais, min)), start, SaoPaulo))
    }

    private fun androidx.compose.ui.test.ComposeUiTest.esperaTag(tag: String) =
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag(tag).assertExists() }.isSuccess }

    @Test
    fun saudacaoComNome() = runComposeUiTest {
        val g = TestGraph()
        runBlocking { g.repository.putSetting(SettingKeys.PROFILE_NAME, "Carla") }
        setContent { TestHost(g.graph) { HomeScreen() } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("home_name").assertTextContains("Carla") }.isSuccess }
        onNodeWithTag("home_greeting").assertTextContains("Bom dia,")
    }

    @Test
    fun saudacaoDeNoite() = runComposeUiTest {
        val g = TestGraph(now = saoPaulo(20, 19))
        setContent { TestHost(g.graph) { HomeScreen() } }
        esperaTag("home_greeting")
        onNodeWithTag("home_greeting").assertTextContains("Boa noite,")
    }

    @Test
    fun previstoERecebido() = runComposeUiTest {
        val g = TestGraph()
        val c = g.cliente("Maria Souza")
        runBlocking {
            val r = g.repository
            r.save(AppointmentRules.book(c, listOf(svc(250)), saoPaulo(20, 13), SaoPaulo))
            r.save(AppointmentRules.confirm(AppointmentRules.book(c, listOf(svc(80)), saoPaulo(20, 16), SaoPaulo)))
            r.save(AppointmentRules.complete(AppointmentRules.book(c, listOf(svc(70)), saoPaulo(20, 8), SaoPaulo), Money.ofReais(70), saoPaulo(20, 9), ""))
            r.save(AppointmentRules.unschedule(AppointmentRules.book(c, listOf(svc(100)), saoPaulo(20, 11), SaoPaulo), CancelReason.CANCELOU))
            r.save(AppointmentRules.complete(AppointmentRules.book(c, listOf(svc(60)), saoPaulo(19, 15), SaoPaulo), Money.ofReais(60), saoPaulo(20, 9, 30), ""))
        }
        setContent { TestHost(g.graph) { HomeScreen() } }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("day_summary").assertTextContains("previsto R$ 330 · recebido R$ 130") }.isSuccess }
    }

    @Test
    fun escolherOutroDia() = runComposeUiTest {
        val g = TestGraph()
        val c = g.cliente("Maria Souza")
        val hoje = g.agenda(c, saoPaulo(20, 13), 250)
        val dia25 = g.agenda(c, saoPaulo(25, 15), 80)
        setContent { TestHost(g.graph) { HomeScreen() } }
        esperaTag("card_$hoje")

        onNodeWithTag("day_25").performClick()

        esperaTag("card_$dia25")
        onNodeWithTag("cards_title").assertTextContains("Atendimentos de 25/11")
        onNodeWithTag("card_$hoje").assertDoesNotExist()
    }

    @Test
    fun avisoSemBaixa() = runComposeUiTest {
        val g = TestGraph()
        val c = g.cliente("Maria Souza")
        g.agenda(c, saoPaulo(19, 13), 50)
        g.agenda(c, saoPaulo(19, 15), 50)
        setContent { TestHost(g.graph) { HomeScreen() } }
        esperaTag("notice_settlement")
        onNodeWithTag("notice_settlement").assertTextContains("2 atendimentos sem baixa", substring = true)
    }

    @Test
    fun aniversarioDeHoje() = runComposeUiTest {
        val g = TestGraph()
        val c = g.cliente("Maria Souza", Birthday(20, 11))
        setContent { TestHost(g.graph) { HomeScreen() } }
        esperaTag("notice_birthday_$c")
        onNodeWithTag("birthday_whatsapp_$c").performClick()
        waitUntil(timeoutMillis = 5_000) { g.messaging.opened.isNotEmpty() }
        kotlin.test.assertEquals("Parabéns, Maria! Feliz aniversário! 🎉", g.messaging.opened.single().text)
    }

    @Test
    fun desmarcadoSomeNaHora() = runComposeUiTest {
        val g = TestGraph()
        val c = g.cliente("Maria Souza")
        val id = g.agenda(c, saoPaulo(20, 13), 250)
        setContent { TestHost(g.graph) { HomeScreen() } }
        esperaTag("card_$id")
        onNodeWithTag("day_summary").assertTextContains("previsto R$ 250 · recebido R$ 0")

        runBlocking { g.repository.save(AppointmentRules.unschedule(g.repository.appointment(id).first()!!, CancelReason.CANCELOU)) }

        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("card_$id").assertDoesNotExist() }.isSuccess }
        onNodeWithTag("day_summary").assertTextContains("previsto R$ 0 · recebido R$ 0")
    }
}
