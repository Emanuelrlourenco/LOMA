// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.Navigator
import app.loma.ui.theme.LomaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.runBlocking

/** Entrega grafo, pilha e tema a uma tela isolada nos testes. */
@Composable
fun TestHost(graph: LomaGraph, navigator: Navigator = Navigator(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLomaGraph provides graph, LocalNavigator provides navigator) {
        LomaTheme(darkTheme = false) {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.fillMaxSizeCompat().background(app.loma.ui.theme.LocalLomaColors.current.background),
            ) { content() }
        }
    }
}

/** Espera o banco (que grava em outra thread) chegar ao estado esperado. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.waitForData(timeoutMillis: Long = 5_000, condition: suspend () -> Boolean) {
    waitUntil(timeoutMillis = timeoutMillis) { runBlocking { condition() } }
}

/** Espera aparecer um texto na tela (os fluxos do Room chegam em outra thread). */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.waitForText(text: String, substring: Boolean = false, timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis = timeoutMillis) { onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }
}

private fun androidx.compose.ui.Modifier.fillMaxSizeCompat() = this.fillMaxSize()
