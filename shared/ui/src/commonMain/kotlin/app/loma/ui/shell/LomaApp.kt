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
import androidx.compose.runtime.CompositionLocalProvider
import app.loma.ui.LocalLomaGraph
import app.loma.ui.LomaGraph
import androidx.compose.runtime.key
import app.loma.ui.appointments.AppointmentDetailScreen
import app.loma.ui.appointments.AppointmentEditScreen
import app.loma.ui.clients.ClientDetailScreen
import app.loma.ui.clients.ClientEditScreen
import app.loma.ui.clients.ClientsScreen
import app.loma.ui.intro.LomaIntro
import app.loma.ui.settings.MessageTemplateScreen
import app.loma.ui.settings.ServiceEditScreen
import app.loma.ui.settings.ServicesScreen
import app.loma.ui.settings.SettingsScreen
import app.loma.ui.intro.rememberReducedMotion
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LomaTheme
import org.jetbrains.compose.resources.stringResource

/** Raiz do app, usada por Android e iOS. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LomaApp(
    graph: LomaGraph,
    darkTheme: Boolean = isSystemInDarkTheme(),
    showIntro: Boolean = true,
    soundEnabled: Boolean = true,
) {
    val navigator = rememberNavigator()
    CompositionLocalProvider(LocalLomaGraph provides graph, LocalNavigator provides navigator) {
        LomaRoot(navigator, darkTheme, showIntro, soundEnabled)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LomaRoot(navigator: Navigator, darkTheme: Boolean, showIntro: Boolean, soundEnabled: Boolean) {
    LomaTheme(darkTheme = darkTheme) {
        var tabName by rememberSaveable { mutableStateOfName(LomaTab.Inicio) }
        // A abertura toca só na abertura do zero: girar a tela ou recriar a Activity não repete.
        var introDone by rememberSaveable { androidx.compose.runtime.mutableStateOf(!showIntro) }
        val tab = LomaTab.valueOf(tabName)
        // Voltar fecha primeiro a tela empilhada; sem telas, vale a regra das abas.
        BackHandler(enabled = navigator.stack.isNotEmpty() || tab != LomaTab.Inicio) {
            if (!navigator.pop()) tab.back()?.let { tabName = it.name }
        }
        val top = navigator.stack.lastOrNull()
        // A ficha da cliente aparece com a barra inferior (aba Clientes), como na tela aprovada.
        val tabOfRoute = if (top is Route.ClientDetail) LomaTab.Clientes else null
        Box(Modifier.fillMaxSize().background(LocalLomaColors.current.background)) {
            if (top == null || tabOfRoute != null) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        if (top == null) TabContent(tab) else RouteContent(top)
                    }
                    LomaBottomBar(
                        selected = tabOfRoute ?: tab,
                        onSelect = {
                            navigator.clear()
                            tabName = it.name
                        },
                    )
                }
            } else {
                RouteContent(top)
            }
        }
        if (!introDone) {
            LomaIntro(
                soundEnabled = soundEnabled,
                reducedMotion = rememberReducedMotion(),
                onFinished = { introDone = true },
            )
        }
    }
}

private fun mutableStateOfName(tab: LomaTab) = androidx.compose.runtime.mutableStateOf(tab.name)

/** Conteúdo de cada aba; as telas reais entram nas tasks seguintes. */
@Composable
private fun TabContent(tab: LomaTab) {
    when (tab) {
        LomaTab.Clientes -> ClientsScreen()
        else -> PlaceholderScreen(stringResource(tab.label))
    }
}

/** Tela do topo da pilha. */
@Composable
private fun RouteContent(route: Route) {
    when (route) {
        Route.Settings -> SettingsScreen()
        Route.Services -> ServicesScreen()
        is Route.ServiceEdit -> key(route) { ServiceEditScreen(route.id) }
        Route.MessageTemplate -> MessageTemplateScreen()
        is Route.ClientDetail -> key(route) { ClientDetailScreen(route.id) }
        is Route.ClientEdit -> key(route) { ClientEditScreen(route.id) }
        is Route.AppointmentDetail -> key(route) { AppointmentDetailScreen(route.id) }
        is Route.AppointmentEdit -> key(route) { AppointmentEditScreen(route.id, route.clientId, route.rescheduleOf, route.day) }
        else -> PlaceholderScreen(route.encode().substringBefore(':'))
    }
}
