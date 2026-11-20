// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LomaAppTest {

    private val tags = listOf("tab_inicio", "tab_agenda", "tab_clientes", "tab_financeiro")

    @BeforeTest
    fun idiomaPortugues() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    }

    @Test
    fun abreNoInicio() = runComposeUiTest {
        setContent { LomaApp() }

        onNodeWithTag("tab_inicio").assertIsSelected()
        onNodeWithTag("screen_title").assertTextEquals("Início")
    }

    @Test
    fun tocarEmFinanceiroMostraAAbaFinanceiro() = runComposeUiTest {
        setContent { LomaApp() }

        onNodeWithTag("tab_financeiro").performClick()

        onNodeWithTag("screen_title").assertTextEquals("Financeiro")
        onNodeWithTag("tab_financeiro").assertIsSelected()
        tags.filter { it != "tab_financeiro" }.forEach { onNodeWithTag(it).assertIsNotSelected() }
    }

    @Test
    fun abaEscolhidaSobreviveARecriacao() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        restoration.setContent { LomaApp() }

        onNodeWithTag("tab_clientes").performClick()
        restoration.emulateSaveAndRestore()

        onNodeWithTag("tab_clientes").assertIsSelected()
        onNodeWithTag("screen_title").assertTextEquals("Clientes")
    }

    @Test
    fun rotulosCabemComFonteEm200PorCento() = runComposeUiTest {
        setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) { LomaApp() }
        }

        listOf("tab_inicio" to "Início", "tab_agenda" to "Agenda", "tab_clientes" to "Clientes", "tab_financeiro" to "Financeiro")
            .forEach { (tag, rotulo) ->
                onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
                onNode(hasText(rotulo) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true).assertIsDisplayed()
            }
    }
}
