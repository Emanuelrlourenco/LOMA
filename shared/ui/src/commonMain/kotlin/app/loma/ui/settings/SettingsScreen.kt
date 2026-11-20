// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import app.loma.ui.generated.resources.client_photo_remove
import app.loma.ui.generated.resources.client_photo_pick
import app.loma.ui.components.PillKind
import app.loma.ui.clients.rememberPhotoPicker
import app.loma.ui.clients.ClientAvatar
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
    val graph = LocalLomaGraph.current
    val repo = graph.repository
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
                val photo by remember { repo.setting(SettingKeys.PROFILE_PHOTO) }.collectAsState(initial = null)
                val current = photo?.takeIf { it.isNotBlank() }
                // A foto vale na hora; trocar ou remover apaga o arquivo anterior.
                fun setPhoto(file: String?) = scope.launch {
                    repo.putSetting(SettingKeys.PROFILE_PHOTO, file.orEmpty())
                    current?.let { if (it != file) graph.photos.delete(it) }
                }
                val photoPicker = rememberPhotoPicker { uri ->
                    if (uri != null) scope.launch { graph.photos.importPhoto(uri)?.let { setPhoto(it) } }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClientAvatar(name.ifBlank { "?" }.take(1), current, size = 72)
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (photoPicker != null) {
                            PillButton(stringResource(Res.string.client_photo_pick), onClick = photoPicker, kind = PillKind.Secondary, testTag = "profile_photo_pick")
                        }
                        if (current != null) {
                            PillButton(stringResource(Res.string.client_photo_remove), onClick = { setPhoto(null) }, kind = PillKind.Danger, testTag = "profile_photo_remove")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
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
