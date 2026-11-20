// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.data.SettingKeys
import app.loma.salon.Appointment
import app.loma.salon.AppointmentStatus
import app.loma.salon.CancelReason
import app.loma.salon.ClientStats
import app.loma.salon.PhoneBr
import app.loma.salon.formatBrl
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.preferredMessagingApp
import app.loma.ui.components.twoDigits
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.back
import app.loma.ui.generated.resources.birthday_short
import app.loma.ui.generated.resources.cancel
import app.loma.ui.generated.resources.client_allergy
import app.loma.ui.generated.resources.client_book
import app.loma.ui.generated.resources.client_call
import app.loma.ui.generated.resources.client_cancellations
import app.loma.ui.generated.resources.client_detail_title
import app.loma.ui.generated.resources.client_history
import app.loma.ui.generated.resources.client_history_empty
import app.loma.ui.generated.resources.client_more
import app.loma.ui.generated.resources.client_notes
import app.loma.ui.generated.resources.client_preferences
import app.loma.ui.generated.resources.client_whatsapp
import app.loma.ui.generated.resources.delete_text
import app.loma.ui.generated.resources.delete_title
import app.loma.ui.generated.resources.history_unscheduled
import app.loma.ui.generated.resources.menu_archive
import app.loma.ui.generated.resources.menu_delete
import app.loma.ui.generated.resources.menu_edit
import app.loma.ui.generated.resources.menu_unarchive
import app.loma.ui.generated.resources.months_short
import app.loma.ui.generated.resources.reason_cancelou
import app.loma.ui.generated.resources.reason_nao_compareceu
import app.loma.ui.generated.resources.reason_remarcou
import app.loma.ui.generated.resources.stat_last
import app.loma.ui.generated.resources.stat_ticket
import app.loma.ui.generated.resources.stat_total
import app.loma.ui.generated.resources.stat_visits
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.LomaIcons
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import app.loma.ui.theme.LomaShapes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

internal fun CancelReason.label(): StringResource = when (this) {
    CancelReason.CANCELOU -> Res.string.reason_cancelou
    CancelReason.NAO_COMPARECEU -> Res.string.reason_nao_compareceu
    CancelReason.REMARCOU -> Res.string.reason_remarcou
}

/** Ficha da cliente, como a tela aprovada T-Cliente. */
@Composable
fun ClientDetailScreen(id: Long) {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val zone = graph.zone()
    val client by remember(id) { repo.client(id) }.collectAsState(initial = null)
    val history by remember(id) { repo.appointmentsOfClient(id) }.collectAsState(initial = emptyList())
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val c = client
    val stats = ClientStats.of(history)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoundIcon(stringResource(Res.string.back), "back", { nav.pop() }) { Icon(LomaIcons.Voltar, null, tint = colors.textPrimary) }
            Text(stringResource(Res.string.client_detail_title), Modifier.weight(1f), style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = colors.textPrimary)
            Box {
                RoundIcon(stringResource(Res.string.client_more), "client_menu", { menuOpen = true }) {
                    Text("⋯", style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp), color = colors.textPrimary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.menu_edit)) }, onClick = { menuOpen = false; nav.push(Route.ClientEdit(id)) }, modifier = Modifier.testTag("menu_edit"))
                    DropdownMenuItem(
                        text = { Text(stringResource(if (c?.archived == true) Res.string.menu_unarchive else Res.string.menu_archive)) },
                        onClick = { menuOpen = false; scope.launch { repo.setArchived(id, c?.archived != true) } },
                        modifier = Modifier.testTag("menu_archive"),
                    )
                    DropdownMenuItem(text = { Text(stringResource(Res.string.menu_delete), color = colors.alert) }, onClick = { menuOpen = false; confirmDelete = true }, modifier = Modifier.testTag("menu_delete"))
                }
            }
        }
        if (c == null) return@Column
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            // Cabeçalho: avatar, nome, telefone e aniversário, botões.
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().background(colors.surface, RoundedCornerShape(28.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClientAvatar(c.name, c.photoFile)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, Modifier.testTag("client_name"), style = type.cardName.copy(lineHeight = 27.sp), color = colors.textPrimary)
                        val bday = c.birthday?.let { stringResource(Res.string.birthday_short, "${twoDigits(it.day)}/${twoDigits(it.month)}") }
                        val line = listOfNotNull(c.phoneDigits?.let { PhoneBr.format(it) }, bday).joinToString(" · ")
                        if (line.isNotEmpty()) Text(line, style = type.body.copy(fontSize = 14.sp), color = colors.textSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val phone = c.phoneDigits
                    SmallPill(stringResource(Res.string.client_whatsapp), colors.accent, colors.onAccent, null, "client_whatsapp", Modifier.weight(1f), enabled = phone != null) {
                        scope.launch {
                            val app = preferredMessagingApp(graph.messaging.available(), repo.setting(SettingKeys.WHATSAPP_APP).first())
                            if (app != null && phone != null) graph.messaging.open(app, phone, "")
                        }
                    }
                    SmallPill(stringResource(Res.string.client_call), Color.Transparent, colors.textPrimary, colors.textPrimary, "client_call", Modifier.weight(1f), enabled = phone != null) {
                        phone?.let { graph.messaging.call(it) }
                    }
                    SmallPill(stringResource(Res.string.client_book), colors.primary, colors.onPrimary, null, "client_book", Modifier.weight(1f)) {
                        nav.push(Route.AppointmentEdit(null, id, null))
                    }
                }
            }
            if (c.allergies.isNotBlank()) {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().testTag("allergy_band").semantics(mergeDescendants = true) {}
                        .background(LomaPalette.AlertSoft, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⚠", color = LomaPalette.OnAlertSoft, style = type.body)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(stringResource(Res.string.client_allergy)) }
                            append(" ")
                            append(c.allergies)
                        },
                        style = type.body.copy(fontSize = 14.sp),
                        color = LomaPalette.OnAlertSoft,
                    )
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val last = stats.lastVisit?.toLocalDateTime(zone)?.date?.let { "${twoDigits(it.day)}/${twoDigits(it.month.ordinal + 1)}" } ?: "—"
                Stat(stringResource(Res.string.stat_last), last, "stat_last", Modifier.weight(1f))
                Stat(stringResource(Res.string.stat_visits), stats.visits.toString(), "stat_visits", Modifier.weight(1f))
                Stat(stringResource(Res.string.stat_total), stats.total.formatBrl(showZeroCents = false), "stat_total", Modifier.weight(1f))
                Stat(stringResource(Res.string.stat_ticket), stats.averageTicket?.formatBrl(showZeroCents = false) ?: "—", "stat_ticket", Modifier.weight(1f))
            }
            TextBlock(stringResource(Res.string.client_preferences), c.preferences)
            TextBlock(stringResource(Res.string.client_notes), c.notes)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Text(stringResource(Res.string.client_history), Modifier.padding(start = 4.dp, bottom = 8.dp), style = type.body.copy(fontWeight = FontWeight.ExtraBold), color = colors.textPrimary)
                if (stats.cancellations > 0) {
                    Text(stringResource(Res.string.client_cancellations, stats.cancellations), Modifier.padding(start = 4.dp, bottom = 8.dp), style = type.body.copy(fontSize = 13.sp), color = colors.textSecondary)
                }
                val shown = history.filter { it.status == AppointmentStatus.CONCLUIDO || it.status == AppointmentStatus.DESMARCADO }
                if (shown.isEmpty()) Text(stringResource(Res.string.client_history_empty), Modifier.padding(start = 4.dp), style = type.body, color = colors.textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { HistoryItem(it, zone) { nav.push(Route.AppointmentDetail(it.id)) } }
                }
            }
        }
    }
    if (confirmDelete && c != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_title, c.name)) },
            text = { Text(stringResource(Res.string.delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            repo.deleteClient(id)?.let { graph.photos.delete(it) }
                            nav.pop()
                        }
                    },
                    modifier = Modifier.testTag("delete_confirm"),
                ) { Text(stringResource(Res.string.menu_delete), color = colors.alert) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, modifier = Modifier.testTag("delete_cancel")) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun RoundIcon(description: String, tag: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(48.dp).testTag(tag).semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(42.dp).background(LocalLomaColors.current.surface, LomaShapes.Pill), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun SmallPill(text: String, bg: Color, fg: Color, border: Color?, tag: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 44.dp).testTag(tag)
            .background(bg, RoundedCornerShape(22.dp))
            .let { if (border != null) it.border(1.5.dp, border, RoundedCornerShape(22.dp)) else it }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = if (enabled) fg else fg.copy(alpha = 0.5f))
    }
}

@Composable
private fun Stat(label: String, value: String, tag: String, modifier: Modifier) {
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    Column(
        modifier.background(colors.surface, RoundedCornerShape(18.dp)).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = type.body.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = colors.textSecondary, textAlign = TextAlign.Center)
        Text(value, Modifier.testTag(tag), style = type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold), color = colors.textPrimary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TextBlock(title: String, text: String) {
    if (text.isBlank()) return
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
        Text(title, style = type.body.copy(fontWeight = FontWeight.ExtraBold), color = colors.textPrimary)
        Text(text, Modifier.padding(top = 4.dp), style = type.body.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Color(0xFF3A3966).takeIf { colors.background == LomaPalette.Background } ?: colors.textSecondary)
    }
}

@Composable
private fun HistoryItem(a: Appointment, zone: TimeZone, onClick: () -> Unit) {
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val months = stringArrayResource(Res.array.months_short)
    val date = a.start.toLocalDateTime(zone).date
    Row(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(44.dp).background(colors.container, RoundedCornerShape(12.dp)).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.day.toString(), style = type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp), color = colors.textPrimary)
            Text(months.getOrElse(date.month.ordinal) { "" }, style = type.body.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                Text(a.services.joinToString(" + ") { it.name }, Modifier.weight(1f), style = type.body.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                val v = if (a.status == AppointmentStatus.CONCLUIDO) a.finalValue?.formatBrl(showZeroCents = false).orEmpty() else ""
                Text(v, style = type.body.copy(fontWeight = FontWeight.ExtraBold), color = colors.textPrimary)
            }
            if (a.status == AppointmentStatus.DESMARCADO) {
                Text(stringResource(Res.string.history_unscheduled, a.cancelReason?.let { stringResource(it.label()) } ?: ""), style = type.body.copy(fontSize = 13.sp), color = colors.textSecondary)
            }
            if (a.note.isNotBlank()) Text(a.note, style = type.body.copy(fontSize = 13.sp), color = colors.textSecondary)
        }
    }
}
