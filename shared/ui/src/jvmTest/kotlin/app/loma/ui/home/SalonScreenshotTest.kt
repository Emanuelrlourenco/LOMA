// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.loma.data.SettingKeys
import app.loma.salon.AppointmentRules
import app.loma.salon.Birthday
import app.loma.salon.Client
import app.loma.salon.Money
import app.loma.salon.Service
import app.loma.ui.SaoPaulo
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.appointments.AppointmentDetailScreen
import app.loma.ui.appointments.CompleteAppointmentScreen
import app.loma.ui.clients.ClientDetailScreen
import app.loma.ui.saoPaulo
import app.loma.ui.shell.LomaApp
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

/** Capturas com os mesmos dados das telas aprovadas (Maria Souza 13:00, Lorena Dias 16:00). */
@OptIn(ExperimentalTestApi::class)
class SalonScreenshotTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private class Exemplo(val g: TestGraph, val maria: Long, val atendimento: Long)

    private fun exemplo(): Exemplo {
        val g = TestGraph()
        return runBlocking {
            val r = g.repository
            r.putSetting(SettingKeys.PROFILE_NAME, "Carla")
            val col = Service(name = "Coloração", durationMinutes = 90, price = Money.ofReais(180), colorArgb = 0xFF5ED6B4)
            val esc = Service(name = "Escova", durationMinutes = 60, price = Money.ofReais(70), colorArgb = 0xFFB69CF2, sortOrder = 1)
            val man = Service(name = "Manicure e pedicure", durationMinutes = 75, price = Money.ofReais(80), colorArgb = 0xFFF29CC3, sortOrder = 2)
            val cs = col.copy(id = r.saveService(col))
            val es = esc.copy(id = r.saveService(esc))
            val ms = man.copy(id = r.saveService(man))
            val maria = r.saveClient(
                Client(
                    name = "Maria Souza", phoneDigits = "5511987654321", birthday = Birthday(14, 3),
                    allergies = "amônia — usar coloração sem amônia",
                    preferences = "Prefere horários depois do almoço. Café sem açúcar. Não gosta de secador muito quente.",
                    createdAt = saoPaulo(1, 9, month = 1),
                ),
            )
            val lorena = r.saveClient(Client(name = "Lorena Dias", phoneDigits = "5511912345678", createdAt = saoPaulo(1, 9, month = 1)))
            val id = r.save(AppointmentRules.book(maria, listOf(cs, es), saoPaulo(20, 13), SaoPaulo).copy(note = "Última fórmula: 7.1 + oxidante 20 vol, 35 min. Levar cartela de tons."))
            r.save(AppointmentRules.confirm(AppointmentRules.book(lorena, listOf(ms), saoPaulo(20, 16), SaoPaulo)))
            r.save(AppointmentRules.complete(AppointmentRules.book(lorena, listOf(es), saoPaulo(20, 8), SaoPaulo), Money.ofReais(70), saoPaulo(20, 9), ""))
            listOf(9 to 230L, 23 to 250L).forEach { (d, v) ->
                r.save(AppointmentRules.complete(AppointmentRules.book(maria, listOf(cs, es), saoPaulo(d, 13, month = 10), SaoPaulo), Money.ofReais(v), saoPaulo(d, 16, month = 10), "Fórmula 7.1 + 20 vol, 35 min."))
            }
            Exemplo(g, maria, id)
        }
    }

    private fun shoot(name: String, tag: String, content: @Composable (Exemplo) -> Unit) = runDesktopComposeUiTest(width = 390, height = 844) {
        val e = exemplo()
        setContent { content(e) }
        espera(tag)
        onRoot().captureRoboImage("screenshots/$name.png")
    }

    private fun ComposeUiTest.espera(tag: String) =
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag(tag).assertExists() }.isSuccess }

    @Test
    fun inicioClaro() = shoot("inicio-claro", "day_summary") { LomaApp(it.g.graph, darkTheme = false, showIntro = false) }

    @Test
    fun inicioEscuro() = shoot("inicio-escuro", "day_summary") { LomaApp(it.g.graph, darkTheme = true, showIntro = false) }

    @Test
    fun fichaClaro() = shoot("ficha-claro", "stat_ticket") { TestHost(it.g.graph) { ClientDetailScreen(it.maria) } }

    @Test
    fun atendimentoClaro() = shoot("atendimento-claro", "confirm_whatsapp") { TestHost(it.g.graph) { AppointmentDetailScreen(it.atendimento) } }

    @Test
    fun concluirClaro() = shoot("concluir-claro", "complete_confirm") { TestHost(it.g.graph) { CompleteAppointmentScreen(it.atendimento) } }
}
