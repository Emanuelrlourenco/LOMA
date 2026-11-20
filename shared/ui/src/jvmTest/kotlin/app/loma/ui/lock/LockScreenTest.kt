// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.loma.platform.AuthKind
import app.loma.ui.TestGraph
import app.loma.ui.shell.LomaApp
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class LockScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    private fun ComposeUiTest.existe(tag: String) = onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun ComposeUiTest.espera(tag: String) = waitUntil(timeoutMillis = 5_000) { existe(tag) }
    private fun ComposeUiTest.esperaSumir(tag: String) = waitUntil(timeoutMillis = 5_000) { !existe(tag) }
    private fun ComposeUiTest.digita(pin: String) = pin.forEach { onNodeWithTag("pin_key_$it").performClick() }
    private fun ComposeUiTest.mensagem(texto: String) = waitUntil(timeoutMillis = 5_000) {
        runCatching { onNodeWithTag("lock_message").assertTextContains(texto, substring = true) }.isSuccess
    }

    @Test
    fun comPinAbreBloqueado() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { LomaApp(g.graph, showIntro = false) }
        espera("lock_screen")
        assertTrue(!existe("tab_inicio"), "o Início não pode aparecer por trás do bloqueio")
    }

    @Test
    fun pinCertoDesbloqueia() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { LomaApp(g.graph, showIntro = false) }
        espera("lock_screen")
        digita("1234")
        esperaSumir("lock_screen")
        espera("tab_inicio")
    }

    @Test
    fun pinErradoAvisa() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { LomaApp(g.graph, showIntro = false) }
        espera("lock_screen")
        digita("0000")
        mensagem("PIN incorreto. Restam 4 tentativas.")
        assertTrue(g.lock.state.value.locked)
    }

    @Test
    fun digitalAbreSozinhaEDesbloqueia() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        g.lock.setBiometric(true)
        setContent { LomaApp(g.graph, showIntro = false) }
        esperaSumir("lock_screen")
        espera("tab_inicio")
        assertEquals(listOf(AuthKind.BIOMETRIC), g.deviceAuth.calls)
    }

    @Test
    fun digitalCanceladaFicaNoPin() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        g.lock.setBiometric(true)
        g.deviceAuth.result = false
        setContent { LomaApp(g.graph, showIntro = false) }
        waitUntil(timeoutMillis = 5_000) { g.deviceAuth.calls.size == 1 }
        espera("lock_screen")
        onNodeWithTag("lock_biometric").performClick()
        waitUntil(timeoutMillis = 5_000) { g.deviceAuth.calls.size == 2 }
        assertTrue(g.lock.state.value.locked)
    }

    @Test
    fun esqueciPinCriaOutro() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        setContent { LomaApp(g.graph, showIntro = false) }
        espera("lock_screen")
        onNodeWithTag("lock_forgot").performClick()
        espera("pin_setup_title")
        assertEquals(listOf(AuthKind.DEVICE), g.deviceAuth.calls)
        digita("5678")
        waitUntil(timeoutMillis = 5_000) {
            runCatching { onNodeWithTag("pin_setup_title").assertTextContains("Digite o PIN de novo") }.isSuccess
        }
        digita("5678")
        espera("tab_inicio")
        runBlocking {
            assertEquals(PinCheck.Wrong(4), g.lock.check("1234"))
            assertEquals(PinCheck.Ok, g.lock.check("5678"))
        }
    }

    @Test
    fun esqueciCanceladoMantemPin() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        g.deviceAuth.result = false
        val antes = g.store.get("pin")
        setContent { LomaApp(g.graph, showIntro = false) }
        espera("lock_screen")
        onNodeWithTag("lock_forgot").performClick()
        waitUntil(timeoutMillis = 5_000) { g.deviceAuth.calls.size == 1 }
        waitForIdle()
        assertTrue(!existe("pin_setup_title"))
        assertTrue(g.lock.state.value.locked)
        assertEquals(antes, g.store.get("pin"))
    }

    @Test
    fun semBloqueioNoCelularExplica() = runComposeUiTest {
        val g = TestGraph(pin = "1234")
        g.deviceAuth.secure = false
        setContent { LomaApp(g.graph, showIntro = false) }
        espera("lock_screen")
        onNodeWithTag("lock_forgot").performClick()
        mensagem("não tem bloqueio de tela")
        assertEquals(emptyList(), g.deviceAuth.calls)
    }
}
