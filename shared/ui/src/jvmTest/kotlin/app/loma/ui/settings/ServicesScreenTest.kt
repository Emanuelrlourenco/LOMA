// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import app.loma.data.SettingKeys
import app.loma.salon.ConfirmationMessage
import app.loma.salon.Money
import app.loma.salon.Service
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.shell.Navigator
import app.loma.ui.shell.Route
import app.loma.ui.waitForData
import app.loma.ui.waitForText
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class ServicesScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    @Test
    fun salvarServicoGravaEVolta() = runComposeUiTest {
        val g = TestGraph()
        val nav = Navigator(listOf(Route.Services, Route.ServiceEdit(null)))
        setContent { TestHost(g.graph, nav) { ServiceEditScreen(id = null) } }

        onNodeWithTag("service_name").performTextReplacement("Coloração")
        onNodeWithTag("service_duration").performTextReplacement("90")
        onNodeWithTag("service_price").performTextReplacement("180")
        onNodeWithTag("save").performScrollTo().performClick()

        waitForData { g.repository.services().first().isNotEmpty() }
        val s = runBlocking { g.repository.services().first() }.single()
        assertEquals("Coloração", s.name)
        assertEquals(90, s.durationMinutes)
        assertEquals(Money.ofReais(180), s.price)
        waitUntil { nav.stack == listOf<Route>(Route.Services) }
    }

    @Test
    fun listaMostraDuracaoEPreco() = runComposeUiTest {
        val g = TestGraph()
        runBlocking { g.repository.saveService(Service(name = "Coloração", durationMinutes = 90, price = Money.ofReais(180), colorArgb = 0xFF5ED6B4)) }
        setContent { TestHost(g.graph) { ServicesScreen() } }
        waitForText("Coloração")
        waitForText("1h30 · R$ 180")
    }

    @Test
    fun desativarEscondeDoAgendamento() = runComposeUiTest {
        val g = TestGraph()
        val id = runBlocking { g.repository.saveService(Service(name = "Escova", durationMinutes = 60, price = Money.ofReais(70), colorArgb = 0)) }
        setContent { TestHost(g.graph) { ServicesScreen() } }
        waitForText("Escova")

        onNodeWithTag("active_$id").performClick()

        waitForData { g.repository.services().first().isEmpty() }
        assertEquals(listOf("Escova"), runBlocking { g.repository.services(includeInactive = true).first() }.map { it.name })
    }

    @Test
    fun formularioSobreviveARecriacao() = runComposeUiTest {
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var visivel by mutableStateOf(true)
        val g = TestGraph()
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (visivel) TestHost(g.graph) { ServiceEditScreen(id = null) }
            }
        }
        onNodeWithTag("service_name").performTextReplacement("Escova")
        onNodeWithTag("service_price").performTextReplacement("70")

        val salvo = runOnIdle { registry.performSave() }
        runOnIdle { visivel = false }
        runOnIdle {
            registry = SaveableStateRegistry(salvo) { true }
            visivel = true
        }

        onNodeWithTag("service_name").assertTextContains("Escova")
        onNodeWithTag("service_price").assertTextContains("70")
    }

    @Test
    fun perfilGuardaONome() = runComposeUiTest {
        val g = TestGraph()
        setContent { TestHost(g.graph) { SettingsScreen() } }
        onNodeWithTag("profile_name").performTextReplacement("Carla")
        onNodeWithTag("profile_save").performClick()
        waitForData { g.repository.setting(SettingKeys.PROFILE_NAME).first() == "Carla" }
    }

    @Test
    fun modeloDaMensagemComPreviaERestaurar() = runComposeUiTest {
        val g = TestGraph()
        setContent { TestHost(g.graph) { MessageTemplateScreen() } }
        waitForText("Oi Maria! Tudo bem?", substring = true)

        onNodeWithTag("template").performTextReplacement("Olá {nome}, até {quando}!")
        waitForText("Olá Maria, até hoje!")
        onNodeWithTag("save").performScrollTo().performClick()
        waitForData { g.repository.setting(SettingKeys.WHATSAPP_TEMPLATE).first() == "Olá {nome}, até {quando}!" }

        onNodeWithTag("restore_default").performScrollTo().performClick()
        onNodeWithTag("template").assertTextContains(ConfirmationMessage.DEFAULT_TEMPLATE)
    }
}
