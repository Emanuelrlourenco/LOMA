// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.lock.PinCheck
import app.loma.ui.shell.Navigator
import app.loma.ui.shell.Route
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class SecurityScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private fun ComposeUiTest.existe(tag: String) = onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun ComposeUiTest.espera(tag: String) = waitUntil(timeoutMillis = 5_000) { existe(tag) }
    private fun ComposeUiTest.digita(pin: String) = pin.forEach { onNodeWithTag("pin_key_$it").performClick() }
    private fun ComposeUiTest.texto(tag: String, texto: String) = waitUntil(timeoutMillis = 5_000) {
        runCatching { onNodeWithTag(tag).assertTextContains(texto, substring = true) }.isSuccess
    }

    @Test
    fun menuSegurancaNosAjustes() = runComposeUiTest {
        val g = TestGraph()
        val nav = Navigator()
        setContent { TestHost(g.graph, nav) { SettingsScreen() } }
        onNodeWithTag("menu_security").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf<Route>(Route.Security), nav.stack) }
    }

    @Test
    fun ligarPinPedeDuasVezes() = runComposeUiTest {
        val g = TestGraph()
        setContent { TestHost(g.graph) { PinChangeScreen("new") } }
        espera("pin_setup_title")
        digita("1234")
        texto("pin_setup_title", "Digite o PIN de novo")
        digita("5678")
        texto("pin_setup_error", "Os PINs não são iguais")
        digita("1234")
        digita("1234")
        waitUntil(timeoutMillis = 5_000) { g.lock.state.value.enabled }
        assertEquals(PinCheck.Ok, runBlocking { g.lock.check("1234") })
    }

    @Test
    fun avisoSemBloqueioDoCelular() = runComposeUiTest {
        val g = TestGraph()
        g.deviceAuth.secure = false
        setContent { TestHost(g.graph) { SecurityScreen() } }
        texto("security_warning", "não tem bloqueio de tela")
    }

    @Test
    fun avisoComBloqueioDoCelular() = runComposeUiTest {
        val g = TestGraph()
        setContent { TestHost(g.graph) { SecurityScreen() } }
        texto("security_warning", "bloqueio do celular")
    }

    @Test
    fun chaveDigitalSoComDigital() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        g.deviceAuth.biometric = false
        setContent { TestHost(g.graph) { SecurityScreen() } }
        espera("security_change_pin")
        assertTrue(!existe("security_biometric_switch"))
    }

    @Test
    fun ligarDigital() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { TestHost(g.graph) { SecurityScreen() } }
        espera("security_biometric_switch")
        onNodeWithTag("security_biometric_switch").performClick()
        waitUntil(timeoutMillis = 5_000) { g.lock.state.value.biometric }
    }

    @Test
    fun tempoSalva() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { TestHost(g.graph) { SecurityScreen() } }
        onNodeWithTag("security_timeout_300").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { g.lock.state.value.timeoutSeconds == 300 }
    }

    @Test
    fun ligarAbreCriacaoDoPin() = runComposeUiTest {
        val g = TestGraph()
        val nav = Navigator()
        setContent { TestHost(g.graph, nav) { SecurityScreen() } }
        onNodeWithTag("security_pin_switch").performClick()
        runOnIdle { assertEquals(listOf<Route>(Route.PinChange("new")), nav.stack) }
    }

    @Test
    fun trocarPinPedeOAtual() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        val antes = g.store.get("pin")
        setContent { TestHost(g.graph) { PinChangeScreen("change") } }
        espera("pin_verify_title")
        digita("0000")
        texto("pin_verify_error", "PIN incorreto")
        digita("1234")
        espera("pin_setup_title")
        digita("5678")
        digita("5678")
        waitUntil(timeoutMillis = 5_000) { g.store.get("pin") != antes }
        assertEquals(PinCheck.Ok, runBlocking { g.lock.check("5678") })
    }

    @Test
    fun desligarPedePinAtual() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { TestHost(g.graph) { PinChangeScreen("disable") } }
        espera("pin_verify_title")
        digita("1234")
        waitUntil(timeoutMillis = 5_000) { !g.lock.state.value.enabled }
    }
}
