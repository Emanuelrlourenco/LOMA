// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.loma.salon.Birthday
import app.loma.salon.Client
import app.loma.salon.PhoneBr
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.LomaTextField
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.PillButton
import app.loma.ui.components.PillKind
import app.loma.ui.components.SectionLabel
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.birthday_day
import app.loma.ui.generated.resources.birthday_invalid
import app.loma.ui.generated.resources.birthday_month
import app.loma.ui.generated.resources.birthday_year
import app.loma.ui.generated.resources.client_allergies
import app.loma.ui.generated.resources.client_birthday
import app.loma.ui.generated.resources.client_edit_title
import app.loma.ui.generated.resources.client_from_contacts
import app.loma.ui.generated.resources.client_name
import app.loma.ui.generated.resources.client_new_title
import app.loma.ui.generated.resources.client_notes
import app.loma.ui.generated.resources.client_phone
import app.loma.ui.generated.resources.client_photo_pick
import app.loma.ui.generated.resources.client_photo_remove
import app.loma.ui.generated.resources.client_preferences
import app.loma.ui.generated.resources.phone_invalid
import app.loma.ui.generated.resources.save
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Cadastro ou edição da ficha completa da cliente. */
@Composable
fun ClientEditScreen(id: Long?) {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var allergies by rememberSaveable { mutableStateOf("") }
    var preferences by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var existing by remember { mutableStateOf<Client?>(null) }
    LaunchedEffect(id) {
        if (id != null) {
            val c = repo.client(id).first()
            existing = c
            if (!loaded && c != null) {
                name = c.name
                phone = c.phoneDigits?.let { PhoneBr.format(it) }.orEmpty()
                day = c.birthday?.day?.toString().orEmpty()
                month = c.birthday?.month?.toString().orEmpty()
                year = c.birthday?.year?.toString().orEmpty()
                allergies = c.allergies
                preferences = c.preferences
                notes = c.notes
                photo = c.photoFile
                loaded = true
            }
        }
    }
    val contactPicker = rememberContactPicker { picked ->
        if (picked != null) {
            if (picked.name.isNotBlank()) name = picked.name
            picked.phone?.let { phone = it }
        }
    }
    val photoPicker = rememberPhotoPicker { uri ->
        if (uri != null) scope.launch { graph.photos.importPhoto(uri)?.let { photo = it } }
    }

    val phoneDigits = PhoneBr.normalize(phone)
    val phoneError = phone.isNotBlank() && phoneDigits == null
    val d = day.toIntOrNull()
    val m = month.toIntOrNull()
    val y = year.toIntOrNull()
    val noBirthday = day.isBlank() && month.isBlank() && year.isBlank()
    val birthdayOk = noBirthday || (d != null && d in 1..31 && m != null && m in 1..12 && (year.isBlank() || (y != null && y in 1900..2100)))
    val valid = name.isNotBlank() && !phoneError && birthdayOk

    Column(Modifier.fillMaxSize()) {
        LomaTopBar(stringResource(if (id == null) Res.string.client_new_title else Res.string.client_edit_title), onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (contactPicker != null) {
                PillButton(stringResource(Res.string.client_from_contacts), onClick = contactPicker, kind = PillKind.Secondary, testTag = "from_contacts", modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClientAvatar(name.ifBlank { "?" }, photo, size = 56)
                Spacer(Modifier.width(12.dp))
                if (photoPicker != null) {
                    PillButton(stringResource(Res.string.client_photo_pick), onClick = photoPicker, kind = PillKind.Secondary, testTag = "photo_pick")
                }
                if (photo != null) {
                    Spacer(Modifier.width(8.dp))
                    PillButton(stringResource(Res.string.client_photo_remove), onClick = { photo = null }, kind = PillKind.Danger, testTag = "photo_remove")
                }
            }
            LomaTextField(name, { name = it }, stringResource(Res.string.client_name), testTag = "client_name")
            LomaTextField(
                phone, { phone = it }, stringResource(Res.string.client_phone), testTag = "client_phone",
                keyboardType = KeyboardType.Phone, error = if (phoneError) stringResource(Res.string.phone_invalid) else null,
            )
            SectionLabel(stringResource(Res.string.client_birthday))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LomaTextField(day, { day = it.filter(Char::isDigit).take(2) }, stringResource(Res.string.birthday_day), Modifier.weight(1f), "birthday_day", KeyboardType.Number)
                LomaTextField(month, { month = it.filter(Char::isDigit).take(2) }, stringResource(Res.string.birthday_month), Modifier.weight(1f), "birthday_month", KeyboardType.Number)
                LomaTextField(year, { year = it.filter(Char::isDigit).take(4) }, stringResource(Res.string.birthday_year), Modifier.weight(1.4f), "birthday_year", KeyboardType.Number)
            }
            if (!birthdayOk) Text(stringResource(Res.string.birthday_invalid), color = colors.alert)
            LomaTextField(allergies, { allergies = it }, stringResource(Res.string.client_allergies), testTag = "client_allergies", singleLine = false)
            LomaTextField(preferences, { preferences = it }, stringResource(Res.string.client_preferences), testTag = "client_preferences", singleLine = false)
            LomaTextField(notes, { notes = it }, stringResource(Res.string.client_notes), testTag = "client_notes", singleLine = false)
            Spacer(Modifier.height(8.dp))
            PillButton(
                stringResource(Res.string.save),
                enabled = valid,
                testTag = "save",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        val old = existing
                        val client = Client(
                            id = id ?: 0,
                            name = name.trim(),
                            phoneDigits = phoneDigits,
                            birthday = if (noBirthday) null else Birthday(d!!, m!!, y),
                            photoFile = photo,
                            allergies = allergies.trim(),
                            preferences = preferences.trim(),
                            notes = notes.trim(),
                            createdAt = old?.createdAt ?: graph.clock.now(),
                            archived = old?.archived ?: false,
                        )
                        val savedId = repo.saveClient(client)
                        old?.photoFile?.let { previous -> if (previous != photo) graph.photos.delete(previous) }
                        when {
                            id != null -> nav.pop()
                            nav.stack.dropLast(1).lastOrNull() is Route.AppointmentEdit -> {
                                nav.clientResult = savedId
                                nav.pop()
                            }
                            else -> nav.replaceTop(Route.ClientDetail(savedId))
                        }
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
