// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.salon.Client
import app.loma.salon.PhoneBr
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.LomaCard
import app.loma.ui.components.LomaTextField
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.PillButton
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.client_archived
import app.loma.ui.generated.resources.clients_empty
import app.loma.ui.generated.resources.clients_new
import app.loma.ui.generated.resources.clients_search
import app.loma.ui.generated.resources.clients_show_archived
import app.loma.ui.generated.resources.clients_sort_last
import app.loma.ui.generated.resources.clients_sort_name
import app.loma.ui.generated.resources.clients_title
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaShapes
import org.jetbrains.compose.resources.stringResource

/** Filtra por nome (sem diferenciar maiúsculas) ou por pedaço do telefone (3+ dígitos). */
fun filterClients(clients: List<Client>, query: String): List<Client> {
    val q = query.trim()
    if (q.isEmpty()) return clients
    val digits = q.filter { it.isDigit() }
    return clients.filter { c ->
        c.name.contains(q, ignoreCase = true) || (digits.length >= 3 && c.phoneDigits?.contains(digits) == true)
    }
}

/** Aba Clientes: busca, ordem e lista. */
@Composable
fun ClientsScreen() {
    val repo = LocalLomaGraph.current.repository
    val nav = LocalNavigator.current
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    var query by rememberSaveable { mutableStateOf("") }
    var byLastVisit by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val clients by remember(showArchived) { repo.clients(includeArchived = showArchived) }.collectAsState(initial = emptyList())
    val lastVisits by remember { repo.lastVisits() }.collectAsState(initial = emptyMap())
    val shown = filterClients(clients, query).let { list ->
        if (byLastVisit) list.sortedWith(compareByDescending<Client, Long?>(nullsFirst()) { lastVisits[it.id]?.toEpochMilliseconds() }.thenBy { it.name.lowercase() }) else list
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LomaTopBar(stringResource(Res.string.clients_title), onBack = null)
            Column(Modifier.padding(horizontal = 18.dp)) {
                LomaTextField(query, { query = it }, stringResource(Res.string.clients_search), testTag = "client_search")
                Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(Res.string.clients_sort_name), !byLastVisit, "sort_name") { byLastVisit = false }
                    Chip(stringResource(Res.string.clients_sort_last), byLastVisit, "sort_last") { byLastVisit = true }
                    Chip(stringResource(Res.string.clients_show_archived), showArchived, "show_archived") { showArchived = !showArchived }
                }
            }
            if (shown.isEmpty() && query.isBlank()) {
                Text(stringResource(Res.string.clients_empty), Modifier.padding(22.dp), style = type.body, color = colors.textSecondary)
            }
            LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { c ->
                    LomaCard(onClick = { nav.push(Route.ClientDetail(c.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClientAvatar(c.name, c.photoFile, size = 44)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = colors.textPrimary)
                                c.phoneDigits?.let { Text(PhoneBr.format(it), style = type.body.copy(fontSize = 14.sp), color = colors.textSecondary) }
                            }
                            if (c.archived) Text(stringResource(Res.string.client_archived), style = type.body.copy(fontSize = 12.sp), color = colors.textSecondary)
                        }
                    }
                }
            }
        }
        PillButton(
            stringResource(Res.string.clients_new),
            onClick = { nav.push(Route.ClientEdit(null)) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp).fillMaxWidth(),
            testTag = "new_client",
        )
    }
}

@Composable
internal fun Chip(text: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LocalLomaColors.current
    Text(
        text,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .testTag(tag)
            .background(if (selected) colors.primary else colors.container, LomaShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
        color = if (selected) colors.onPrimary else colors.textPrimary,
    )
}
