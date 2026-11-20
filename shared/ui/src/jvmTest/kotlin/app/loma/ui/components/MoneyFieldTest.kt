// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MoneyFieldTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    @Test
    fun aceitaValoresComMilharECentavosERecusaTexto() = runComposeUiTest {
        var texto by mutableStateOf("")
        setContent { TestHost(TestGraph().graph) { MoneyField(texto, { texto = it }, "Preço", testTag = "preco") } }

        onNodeWithTag("preco").performTextReplacement("R$ 1.420,00")
        onNodeWithText("Valor inválido").assertDoesNotExist()

        onNodeWithTag("preco").performTextReplacement("abc")
        onNodeWithText("Valor inválido").assertExists()
    }
}
