// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.loma.data.SettingKeys
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.LomaCard
import app.loma.ui.components.LomaTextField
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.MenuRow
import app.loma.ui.components.PillButton
import app.loma.ui.components.SectionLabel
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.save
import app.loma.ui.generated.resources.settings_message
import app.loma.ui.generated.resources.settings_message_hint
import app.loma.ui.generated.resources.settings_profile
import app.loma.ui.generated.resources.settings_profile_name
import app.loma.ui.generated.resources.settings_services
import app.loma.ui.generated.resources.settings_services_hint
import app.loma.ui.generated.resources.settings_title
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.Route
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Ajustes: perfil, serviços e mensagem do WhatsApp. */
@Composable
fun SettingsScreen() {
    val repo = LocalLomaGraph.current.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!loaded) {
            name = repo.setting(SettingKeys.PROFILE_NAME).first().orEmpty()
            loaded = true
        }
    }
    Column(Modifier.fillMaxSize()) {
        LomaTopBar(stringResource(Res.string.settings_title), onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionLabel(stringResource(Res.string.settings_profile))
            LomaCard {
                LomaTextField(name, { name = it }, stringResource(Res.string.settings_profile_name), testTag = "profile_name")
                Spacer(Modifier.height(10.dp))
                PillButton(stringResource(Res.string.save), onClick = { scope.launch { repo.putSetting(SettingKeys.PROFILE_NAME, name.trim()) } }, testTag = "profile_save")
            }
            Spacer(Modifier.height(14.dp))
            LomaCard {
                MenuRow(stringResource(Res.string.settings_services), stringResource(Res.string.settings_services_hint), { nav.push(Route.Services) }, "menu_services")
                MenuRow(stringResource(Res.string.settings_message), stringResource(Res.string.settings_message_hint), { nav.push(Route.MessageTemplate) }, "menu_message")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
