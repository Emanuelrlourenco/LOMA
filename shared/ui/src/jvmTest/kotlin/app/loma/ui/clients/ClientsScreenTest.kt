// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import app.loma.salon.Client
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.saoPaulo
import app.loma.ui.waitForText
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class ClientsScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    @Test
    fun buscaPorNomeETelefone() = runComposeUiTest {
        val g = TestGraph()
        runBlocking {
            g.repository.saveClient(Client(name = "Maria Souza", phoneDigits = "5511987654321", createdAt = saoPaulo(1, 9)))
            g.repository.saveClient(Client(name = "Lorena Dias", phoneDigits = "5511912345678", createdAt = saoPaulo(1, 9)))
        }
        setContent { TestHost(g.graph) { ClientsScreen() } }
        waitForText("Maria Souza")
        waitForText("Lorena Dias")

        onNodeWithTag("client_search").performTextReplacement("lor")
        waitUntil { onNodeWithText("Maria Souza").let { runCatching { it.assertDoesNotExist() }.isSuccess } }
        onNodeWithText("Lorena Dias").assertExists()

        onNodeWithTag("client_search").performTextReplacement("98765")
        waitForText("Maria Souza")
        waitUntil { runCatching { onNodeWithText("Lorena Dias").assertDoesNotExist() }.isSuccess }
    }

    @Test
    fun telefoneInvalidoBloqueiaSalvar() = runComposeUiTest {
        val g = TestGraph()
        setContent { TestHost(g.graph) { ClientEditScreen(id = null) } }
        onNodeWithTag("client_name").performTextReplacement("Ana")
        onNodeWithTag("client_phone").performTextReplacement("123")

        onNodeWithText("Número inválido").assertExists()
        onNodeWithTag("save").performScrollTo().assertIsNotEnabled()
        assertTrue(runBlocking { g.repository.clients().first() }.isEmpty())
    }
}
