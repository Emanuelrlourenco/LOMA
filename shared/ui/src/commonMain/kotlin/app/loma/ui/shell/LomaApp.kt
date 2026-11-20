// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LomaTheme
import org.jetbrains.compose.resources.stringResource

/** Raiz do app, usada por Android e iOS. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LomaApp(darkTheme: Boolean = isSystemInDarkTheme()) {
    LomaTheme(darkTheme = darkTheme) {
        var tabName by rememberSaveable { mutableStateOfName(LomaTab.Inicio) }
        val tab = LomaTab.valueOf(tabName)
        BackHandler(enabled = tab != LomaTab.Inicio) {
            tab.back()?.let { tabName = it.name }
        }
        Column(Modifier.fillMaxSize().background(LocalLomaColors.current.background)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                PlaceholderScreen(stringResource(tab.label))
            }
            LomaBottomBar(selected = tab, onSelect = { tabName = it.name })
        }
    }
}

private fun mutableStateOfName(tab: LomaTab) = androidx.compose.runtime.mutableStateOf(tab.name)
