// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.data.SettingKeys
import app.loma.platform.MessagingApp
import app.loma.salon.Appointment
import app.loma.salon.AppointmentRules
import app.loma.salon.AppointmentStatus
import app.loma.salon.CancelReason
import app.loma.salon.ConfirmationMessage
import app.loma.salon.formatBrl
import app.loma.ui.LocalLomaGraph
import app.loma.ui.clients.label
import app.loma.ui.components.formatDuration
import app.loma.ui.components.hhmm
import app.loma.ui.components.preferredMessagingApp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.action_complete
import app.loma.ui.generated.resources.action_confirmed
import app.loma.ui.generated.resources.action_reopen
import app.loma.ui.generated.resources.action_reschedule
import app.loma.ui.generated.resources.action_unschedule
import app.loma.ui.generated.resources.app_business
import app.loma.ui.generated.resources.app_sms
import app.loma.ui.generated.resources.app_whatsapp
import app.loma.ui.generated.resources.appt_menu
import app.loma.ui.generated.resources.appt_note
import app.loma.ui.generated.resources.appt_services
import app.loma.ui.generated.resources.back
import app.loma.ui.generated.resources.cancel
import app.loma.ui.generated.resources.client_allergy
import app.loma.ui.generated.resources.client_confirmed_q
import app.loma.ui.generated.resources.client_removed
import app.loma.ui.generated.resources.confirm_sms
import app.loma.ui.generated.resources.confirm_whatsapp
import app.loma.ui.generated.resources.delete_appt_confirm
import app.loma.ui.generated.resources.menu_delete
import app.loma.ui.generated.resources.menu_edit
import app.loma.ui.generated.resources.no_messaging
import app.loma.ui.generated.resources.no_phone
import app.loma.ui.generated.resources.not_yet
import app.loma.ui.generated.resources.open_with
import app.loma.ui.generated.resources.reason_title_cancelou
import app.loma.ui.generated.resources.reason_title_nao_compareceu
import app.loma.ui.generated.resources.reason_title_remarcou
import app.loma.ui.generated.resources.reopen_confirm
import app.loma.ui.generated.resources.status_confirmed
import app.loma.ui.generated.resources.status_done
import app.loma.ui.generated.resources.status_scheduled
import app.loma.ui.generated.resources.status_unscheduled
import app.loma.ui.generated.resources.unschedule_title
import app.loma.ui.settings.whenLabels
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.LomaIcons
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private val Muted = Color(0xFFC9C6F0)

/** Detalhe do atendimento (tela cheia marinho, como a T-Atendimento) e mudanças de status. */
@Composable
fun AppointmentDetailScreen(id: Long) {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val type = LocalLomaTypography.current
    val zone = graph.zone()
    val appointment by remember(id) { repo.appointment(id) }.collectAsState(initial = null)
    val a = appointment
    val client by remember(a?.clientId) { a?.clientId?.let { repo.client(it) } ?: kotlinx.coroutines.flow.flowOf(null) }.collectAsState(initial = null)

    var undoOf by remember { mutableStateOf<Appointment?>(null) }
    var undoKey by remember { mutableIntStateOf(0) }
    var askConfirmed by remember { mutableStateOf(false) }
    var chooseApp by remember { mutableStateOf(false) }
    var reasonDialog by remember { mutableStateOf(false) }
    var reopenDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    // "Desfazer" vindo da tela de concluir.
    LaunchedEffect(nav.pendingUndo) {
        nav.pendingUndo?.takeIf { it.id == id }?.let {
            undoOf = it
            undoKey++
            nav.pendingUndo = null
        }
    }

    fun change(new: Appointment) {
        val old = a ?: return
        scope.launch {
            repo.save(new)
            undoOf = old
            undoKey++
        }
    }

    val available = remember { graph.messaging.available() }
    val hasWhatsApp = MessagingApp.WHATSAPP in available || MessagingApp.WHATSAPP_BUSINESS in available
    val labels = whenLabels()
    val noPhoneText = stringResource(Res.string.no_phone)
    val noAppText = stringResource(Res.string.no_messaging)

    fun sendWith(app: MessagingApp) {
        val appt = a ?: return
        val phone = client?.phoneDigits ?: return
        scope.launch {
            val template = repo.setting(SettingKeys.WHATSAPP_TEMPLATE).first() ?: ConfirmationMessage.DEFAULT_TEMPLATE
            val today = graph.clock.now().toLocalDateTime(zone).date
            val text = ConfirmationMessage.render(template, client?.name.orEmpty(), appt.start, zone, today, appt.services.map { it.name }, labels)
            if (graph.messaging.open(app, phone, text)) askConfirmed = true
        }
    }

    fun confirmByMessage() {
        if (client?.phoneDigits == null) {
            notice = noPhoneText
            return
        }
        scope.launch {
            val stored = repo.setting(SettingKeys.WHATSAPP_APP).first()
            val storedApp = stored?.let { runCatching { MessagingApp.valueOf(it) }.getOrNull() }
            val both = MessagingApp.WHATSAPP in available && MessagingApp.WHATSAPP_BUSINESS in available
            if (both && (storedApp == null || storedApp !in available)) {
                chooseApp = true
            } else {
                val app = preferredMessagingApp(available, stored)
                if (app == null) notice = noAppText else sendWith(app)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(LomaPalette.Background)) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 14.dp)
                .background(LomaPalette.Navy, RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundNavyButton(stringResource(Res.string.back), "back") { nav.pop() }
                Spacer(Modifier.weight(1f))
                if (a != null) StatusPill(a)
                Spacer(Modifier.size(8.dp))
                Box {
                    Box(
                        Modifier.size(40.dp).testTag("appt_menu").background(LomaPalette.NavyLight, RoundedCornerShape(20.dp))
                            .semantics { contentDescription = "menu" }.clickable(role = Role.Button) { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("⋯", color = Color.White, style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (a?.isOpen == true) {
                            DropdownMenuItem(text = { Text(stringResource(Res.string.menu_edit)) }, onClick = { menuOpen = false; nav.push(Route.AppointmentEdit(id, null, null)) }, modifier = Modifier.testTag("menu_edit"))
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.menu_delete), color = LomaPalette.Coral) },
                            onClick = {
                                menuOpen = false
                                if (a?.status == AppointmentStatus.CONCLUIDO) deleteDialog = true else scope.launch { repo.delete(id); nav.pop() }
                            },
                            modifier = Modifier.testTag("menu_delete"),
                        )
                    }
                }
            }
            if (a == null) return@Column
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
                val name = client?.name ?: stringResource(Res.string.client_removed)
                Text(
                    name.substringBefore(' ') + (name.substringAfter(' ', "").let { if (it.isEmpty()) "" else "\n$it" }),
                    Modifier.padding(top = 22.dp).testTag("appt_client_name"),
                    style = type.screenTitle.copy(fontSize = 44.sp, lineHeight = 45.sp, letterSpacing = (-1.2).sp),
                    color = Color.White,
                )
                Text(a.services.joinToString(" + ") { it.name }.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(top = 8.dp), style = type.body.copy(fontSize = 17.sp), color = Muted)
                val allergies = client?.allergies.orEmpty()
                if (allergies.isNotBlank()) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(stringResource(Res.string.client_allergy)) }
                            append(" ")
                            append(allergies)
                        },
                        Modifier.padding(top = 16.dp).fillMaxWidth().testTag("allergy_band").background(LomaPalette.Coral, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                        style = type.body.copy(fontSize = 14.sp, lineHeight = 19.sp),
                        color = Color(0xFF3A0B03),
                    )
                }
                // Horário mostrado no fuso do aparelho.
                val s = a.start.toLocalDateTime(zone)
                val e = a.end.toLocalDateTime(zone)
                Row(Modifier.padding(top = 20.dp).fillMaxWidth()) {
                    InfoCell("${hhmm(s.hour, s.minute)} – ${hhmm(e.hour, e.minute)}", Modifier.weight(1f), "info_time")
                    InfoCell(dayLabel(s.date), Modifier.weight(1f).border(1.dp, Color(0xFF3A3994)), "info_date")
                    InfoCell((a.finalValue ?: a.expected).formatBrl(showZeroCents = false), Modifier.weight(1f), "info_value")
                }
                Text(stringResource(Res.string.appt_services), Modifier.padding(top = 22.dp, bottom = 8.dp), style = type.body.copy(fontWeight = FontWeight.Bold), color = Muted)
                a.services.forEach { sv ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(sv.name, style = type.body.copy(fontSize = 16.sp), color = Color.White)
                        Text(" · ${formatDuration(sv.durationMinutes)}", Modifier.weight(1f), style = type.body.copy(fontSize = 16.sp), color = Muted)
                        Text(sv.price.formatBrl(showZeroCents = false), style = type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
                if (a.note.isNotBlank()) {
                    Text(stringResource(Res.string.appt_note), Modifier.padding(top = 18.dp, bottom = 6.dp), style = type.body.copy(fontWeight = FontWeight.Bold), color = Muted)
                    Text(a.note, style = type.body.copy(fontSize = 15.sp, lineHeight = 21.sp), color = Color.White)
                }
                notice?.let { Text(it, Modifier.padding(top = 14.dp).testTag("notice"), color = LomaPalette.Mint, style = type.body) }
                if (askConfirmed && a.status == AppointmentStatus.AGENDADO) {
                    Column(Modifier.padding(top = 16.dp).fillMaxWidth().background(LomaPalette.NavyLight, RoundedCornerShape(18.dp)).padding(14.dp)) {
                        Text(stringResource(Res.string.client_confirmed_q), style = type.body.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NavyPill(stringResource(Res.string.action_confirmed), LomaPalette.Mint, LomaPalette.OnMint, null, "confirmed_yes", Modifier.weight(1f)) {
                                askConfirmed = false
                                change(AppointmentRules.confirm(a))
                            }
                            NavyPill(stringResource(Res.string.not_yet), Color.Transparent, Color.White, Color.White, "confirmed_no", Modifier.weight(1f)) { askConfirmed = false }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Column(Modifier.padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (a.status) {
                    AppointmentStatus.AGENDADO -> {
                        NavyPill(stringResource(if (hasWhatsApp || available.isEmpty()) Res.string.confirm_whatsapp else Res.string.confirm_sms), LomaPalette.Mint, LomaPalette.OnMint, null, "confirm_whatsapp", Modifier.fillMaxWidth(), height = 56) { confirmByMessage() }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NavyPill(stringResource(Res.string.action_confirmed), Color.Transparent, Color.White, Color.White, "confirmed", Modifier.weight(1f)) { change(AppointmentRules.confirm(a)) }
                            NavyPill(stringResource(Res.string.action_complete), Color.White, LomaPalette.Navy, null, "complete", Modifier.weight(1f)) { nav.push(Route.CompleteAppointment(id)) }
                        }
                        TextAction(stringResource(Res.string.action_unschedule), "unschedule") { reasonDialog = true }
                    }
                    AppointmentStatus.CONFIRMADO -> {
                        NavyPill(stringResource(Res.string.action_complete), Color.White, LomaPalette.Navy, null, "complete", Modifier.fillMaxWidth(), height = 56) { nav.push(Route.CompleteAppointment(id)) }
                        TextAction(stringResource(Res.string.action_unschedule), "unschedule") { reasonDialog = true }
                    }
                    AppointmentStatus.CONCLUIDO -> NavyPill(stringResource(Res.string.action_reopen), Color.Transparent, Color.White, Color.White, "reopen", Modifier.fillMaxWidth()) { reopenDialog = true }
                    AppointmentStatus.DESMARCADO -> NavyPill(stringResource(Res.string.action_reschedule), LomaPalette.Mint, LomaPalette.OnMint, null, "reschedule", Modifier.fillMaxWidth()) {
                        nav.push(Route.AppointmentEdit(null, null, id))
                    }
                }
            }
        }
        val previous = undoOf
        if (previous != null) {
            UndoSnackbar(
                key = undoKey,
                onUndo = {
                    undoOf = null
                    scope.launch { repo.save(previous) }
                },
                onTimeout = { undoOf = null },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp),
            )
        }
    }

    if (chooseApp) {
        AlertDialog(
            onDismissRequest = { chooseApp = false },
            title = { Text(stringResource(Res.string.open_with)) },
            text = {
                Column {
                    listOf(MessagingApp.WHATSAPP to Res.string.app_whatsapp, MessagingApp.WHATSAPP_BUSINESS to Res.string.app_business, MessagingApp.SMS to Res.string.app_sms)
                        .filter { it.first in available }
                        .forEach { (app, label) ->
                            Text(
                                stringResource(label),
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("app_${app.name}").clickable {
                                    chooseApp = false
                                    scope.launch { repo.putSetting(SettingKeys.WHATSAPP_APP, app.name) }
                                    sendWith(app)
                                }.padding(vertical = 12.dp),
                                style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            )
                        }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseApp = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
    if (reasonDialog && a != null) {
        AlertDialog(
            onDismissRequest = { reasonDialog = false },
            title = { Text(stringResource(Res.string.unschedule_title)) },
            text = {
                Column {
                    listOf(
                        CancelReason.CANCELOU to Res.string.reason_title_cancelou,
                        CancelReason.NAO_COMPARECEU to Res.string.reason_title_nao_compareceu,
                        CancelReason.REMARCOU to Res.string.reason_title_remarcou,
                    ).forEach { (reason, label) ->
                        Text(
                            stringResource(label),
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reason_${reason.name}").clickable {
                                reasonDialog = false
                                val updated = AppointmentRules.unschedule(a, reason)
                                if (reason == CancelReason.REMARCOU) {
                                    scope.launch {
                                        repo.save(updated)
                                        nav.push(Route.AppointmentEdit(null, null, id))
                                    }
                                } else {
                                    change(updated)
                                }
                            }.padding(vertical = 12.dp),
                            style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { reasonDialog = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
    if (reopenDialog && a != null) {
        AlertDialog(
            onDismissRequest = { reopenDialog = false },
            text = { Text(stringResource(Res.string.reopen_confirm)) },
            confirmButton = { TextButton(onClick = { reopenDialog = false; change(AppointmentRules.reopen(a)) }, modifier = Modifier.testTag("reopen_confirm")) { Text(stringResource(Res.string.action_reopen)) } },
            dismissButton = { TextButton(onClick = { reopenDialog = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            text = { Text(stringResource(Res.string.delete_appt_confirm)) },
            confirmButton = {
                TextButton(onClick = { deleteDialog = false; scope.launch { repo.delete(id); nav.pop() } }, modifier = Modifier.testTag("delete_confirm")) {
                    Text(stringResource(Res.string.menu_delete), color = LomaPalette.Coral)
                }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun StatusPill(a: Appointment) {
    val type = LocalLomaTypography.current
    val text = when (a.status) {
        AppointmentStatus.AGENDADO -> stringResource(Res.string.status_scheduled)
        AppointmentStatus.CONFIRMADO -> stringResource(Res.string.status_confirmed)
        AppointmentStatus.CONCLUIDO -> stringResource(Res.string.status_done, (a.finalValue ?: a.expected).formatBrl(showZeroCents = false))
        AppointmentStatus.DESMARCADO -> stringResource(Res.string.status_unscheduled, a.cancelReason?.let { stringResource(it.label()) }.orEmpty())
    }
    val bg = when (a.status) {
        AppointmentStatus.AGENDADO -> LomaPalette.Lilac
        AppointmentStatus.CONFIRMADO -> Color.White
        AppointmentStatus.CONCLUIDO -> LomaPalette.Mint
        AppointmentStatus.DESMARCADO -> LomaPalette.AlertSoft
    }
    Text(
        text,
        Modifier.testTag("status_pill").heightIn(min = 30.dp).background(bg, RoundedCornerShape(15.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        style = type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
        color = LomaPalette.Navy,
    )
}

@Composable
private fun RoundNavyButton(description: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).testTag(tag).semantics { contentDescription = description }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).background(LomaPalette.NavyLight, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(LomaIcons.Voltar, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun InfoCell(text: String, modifier: Modifier, tag: String) {
    Text(
        text,
        modifier.testTag(tag).padding(vertical = 4.dp),
        style = LocalLomaTypography.current.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        color = Color.White,
    )
}

@Composable
internal fun NavyPill(text: String, bg: Color, fg: Color, border: Color?, tag: String, modifier: Modifier, height: Int = 52, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = height.dp).testTag(tag)
            .background(bg, RoundedCornerShape((height / 2).dp))
            .let { if (border != null) it.border(1.5.dp, border, RoundedCornerShape((height / 2).dp)) else it }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalLomaTypography.current.body.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold), color = fg)
    }
}

@Composable
private fun TextAction(text: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag(tag).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = LocalLomaTypography.current.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Color(0xFFFFB3A6))
    }
}
