// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.data.SettingKeys
import app.loma.salon.Appointment
import app.loma.salon.AppointmentStatus
import app.loma.salon.ConfirmationMessage
import app.loma.salon.DaySummary
import app.loma.salon.AppointmentRules
import app.loma.salon.formatBrl
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.formatDuration
import app.loma.ui.components.hhmm
import app.loma.ui.components.preferredMessagingApp
import app.loma.ui.components.twoDigits
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.birthday_message
import app.loma.ui.generated.resources.card_confirmed
import app.loma.ui.generated.resources.card_done
import app.loma.ui.generated.resources.card_not_confirmed
import app.loma.ui.generated.resources.cards_day
import app.loma.ui.generated.resources.cards_today
import app.loma.ui.generated.resources.client_removed
import app.loma.ui.generated.resources.client_whatsapp
import app.loma.ui.generated.resources.action_confirmed
import app.loma.ui.generated.resources.day_summary
import app.loma.ui.generated.resources.greeting_afternoon
import app.loma.ui.generated.resources.greeting_evening
import app.loma.ui.generated.resources.greeting_morning
import app.loma.ui.generated.resources.months_full
import app.loma.ui.generated.resources.new_appointment
import app.loma.ui.generated.resources.no_appointments
import app.loma.ui.generated.resources.notice_birthday
import app.loma.ui.generated.resources.notice_settlement
import app.loma.ui.generated.resources.notice_settlement_one
import app.loma.ui.generated.resources.notices
import app.loma.ui.generated.resources.open_settings
import app.loma.ui.generated.resources.weekdays_header
import app.loma.ui.settings.whenLabels
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.LomaIcons
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

private val MutedOnNavy = Color(0xFFC9C6F0)
private val DotPurple = Color(0xFF8B6FE0)

private fun LocalDate.firstOfMonth() = LocalDate(year, month, 1)

/** Início: o dia do salão (tela aprovada T-Inicio). */
@Composable
fun HomeScreen() {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val zone = graph.zone()
    val now = graph.clock.now().toLocalDateTime(zone)
    val today = now.date

    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = runCatching { LocalDate.parse(selectedText) }.getOrNull() ?: today
    val monthStart = selected.firstOfMonth()
    val nextMonth = monthStart.plus(DatePeriod(months = 1))

    val profileName by remember { repo.setting(SettingKeys.PROFILE_NAME) }.collectAsState(initial = null)
    val dayAppointments by remember(selectedText) { repo.appointmentsOn(selected, zone) }.collectAsState(initial = emptyList())
    val monthAppointments by remember(monthStart) { repo.appointmentsBetween(monthStart, nextMonth, zone) }.collectAsState(initial = emptyList())
    val unsettled by remember { repo.withoutSettlement() }.collectAsState(initial = emptyList())
    val clients by remember { repo.clients() }.collectAsState(initial = emptyList())

    val summary = DaySummary.of(dayAppointments, selected, zone)
    val cards = dayAppointments.filter { it.status != AppointmentStatus.DESMARCADO && it.start.toLocalDateTime(zone).date == selected }
    val busyDays = monthAppointments.filter { it.status != AppointmentStatus.DESMARCADO }.map { it.start.toLocalDateTime(zone).date.day }.toSet()
    val birthdays = clients.filter { it.birthday?.let { b -> b.day == today.day && b.month == today.month.ordinal + 1 } == true }
    val labels = whenLabels()
    val birthdayTemplate = stringResource(Res.string.birthday_message, "%NAME%")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            // Cabeçalho: avatar (Ajustes), sino e saudação.
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)).padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initial = profileName?.trim()?.take(1)?.uppercase().orEmpty().ifEmpty { "L" }
                    Box(
                        Modifier.size(44.dp).testTag("home_avatar").background(LomaPalette.Lilac, RoundedCornerShape(16.dp))
                            .semantics { contentDescription = "" }.clickable(role = Role.Button) { nav.push(Route.Settings) },
                        contentAlignment = Alignment.Center,
                    ) { Text(initial, style = type.body.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = LomaPalette.Navy) }
                    Spacer(Modifier.weight(1f))
                    val notices = stringResource(Res.string.notices)
                    Box(Modifier.size(44.dp).background(colors.surface, RoundedCornerShape(22.dp)).semantics { contentDescription = notices }, contentAlignment = Alignment.Center) {
                        Text("🔔", style = type.body.copy(fontSize = 16.sp))
                        if (unsettled.isNotEmpty() || birthdays.isNotEmpty()) {
                            Box(Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 10.dp).size(9.dp).background(LomaPalette.Coral, RoundedCornerShape(5.dp)).border(2.dp, Color.White, RoundedCornerShape(5.dp)))
                        }
                    }
                }
                val greeting = when (now.hour) {
                    in 5..11 -> Res.string.greeting_morning
                    in 12..17 -> Res.string.greeting_afternoon
                    else -> Res.string.greeting_evening
                }
                Text(stringResource(greeting), Modifier.padding(top = 12.dp).testTag("home_greeting"), style = type.body.copy(fontSize = 24.sp, lineHeight = 26.sp), color = colors.textPrimary)
                profileName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, Modifier.testTag("home_name").semantics { heading() }, style = type.screenTitle.copy(fontSize = 32.sp, lineHeight = 35.sp, letterSpacing = (-0.6).sp), color = colors.textPrimary)
                }
            }
            // Meses em pílulas.
            val monthNames = stringArrayResource(Res.array.months_full)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, top = 14.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (-2..2).forEach { k ->
                    val m = today.firstOfMonth().let { if (k < 0) it.minus(DatePeriod(months = -k)) else it.plus(DatePeriod(months = k)) }
                    val isSel = m == monthStart
                    Text(
                        monthNames.getOrElse(m.month.ordinal) { "" },
                        Modifier.heightIn(min = 34.dp).testTag("month_${m.year}_${m.month.ordinal + 1}")
                            .background(if (isSel) LomaPalette.Navy else Color.Transparent, RoundedCornerShape(17.dp))
                            .border(1.5.dp, if (colors.background == LomaPalette.Background) LomaPalette.Navy else colors.border, RoundedCornerShape(17.dp))
                            .clickable { selectedText = (if (m == today.firstOfMonth()) today else m).toString() }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        style = type.body.copy(fontSize = 14.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold),
                        color = if (isSel) Color.White else colors.textPrimary,
                    )
                }
            }
            MiniMonth(monthStart, today, selected, busyDays) { selectedText = it.toString() }
            // Avisos.
            if (unsettled.isNotEmpty()) {
                Notice(
                    if (unsettled.size == 1) stringResource(Res.string.notice_settlement_one) else stringResource(Res.string.notice_settlement, unsettled.size),
                    "notice_settlement",
                    onClick = { nav.push(Route.AppointmentDetail(unsettled.first().id)) },
                )
            }
            birthdays.forEach { c ->
                Notice(stringResource(Res.string.notice_birthday, c.name), "notice_birthday_${c.id}", onClick = { nav.push(Route.ClientDetail(c.id)) }) {
                    val phone = c.phoneDigits
                    if (phone != null) {
                        SmallButton(stringResource(Res.string.client_whatsapp), LomaPalette.Mint, LomaPalette.OnMint, null, "birthday_whatsapp_${c.id}") {
                            scope.launch {
                                val app = preferredMessagingApp(graph.messaging.available(), repo.setting(SettingKeys.WHATSAPP_APP).first())
                                if (app != null) graph.messaging.open(app, phone, birthdayTemplate.replace("%NAME%", c.name.trim().substringBefore(' ')))
                            }
                        }
                    }
                }
            }
            // Atendimentos do dia.
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selected == today) stringResource(Res.string.cards_today) else stringResource(Res.string.cards_day, "${twoDigits(selected.day)}/${twoDigits(selected.month.ordinal + 1)}"),
                    Modifier.weight(1f).testTag("cards_title"),
                    style = type.body.copy(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(Res.string.day_summary, summary.expected.formatBrl(showZeroCents = false), summary.received.formatBrl(showZeroCents = false)),
                    Modifier.testTag("day_summary"),
                    style = type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                )
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (cards.isEmpty()) {
                    Text(stringResource(Res.string.no_appointments), Modifier.padding(8.dp).testTag("no_appointments"), style = type.body, color = colors.textSecondary)
                }
                cards.forEach { a ->
                    AppointmentCard(
                        a,
                        clientName = clients.firstOrNull { it.id == a.clientId }?.name ?: stringResource(Res.string.client_removed),
                        zone = zone,
                        onOpen = { nav.push(Route.AppointmentDetail(a.id)) },
                        onWhatsApp = {
                            val phone = clients.firstOrNull { it.id == a.clientId }?.phoneDigits ?: return@AppointmentCard
                            scope.launch {
                                val template = repo.setting(SettingKeys.WHATSAPP_TEMPLATE).first() ?: ConfirmationMessage.DEFAULT_TEMPLATE
                                val app = preferredMessagingApp(graph.messaging.available(), repo.setting(SettingKeys.WHATSAPP_APP).first()) ?: return@launch
                                val name = clients.firstOrNull { it.id == a.clientId }?.name.orEmpty()
                                graph.messaging.open(app, phone, ConfirmationMessage.render(template, name, a.start, zone, today, a.services.map { it.name }, labels))
                            }
                        },
                        onConfirm = { scope.launch { repo.save(AppointmentRules.confirm(a)) } },
                    )
                }
            }
        }
        // Botão "+": agenda no dia escolhido.
        val newLabel = stringResource(Res.string.new_appointment)
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 16.dp).size(60.dp).testTag("home_add")
                .background(LomaPalette.Navy, RoundedCornerShape(22.dp))
                .semantics { contentDescription = newLabel }
                .clickable(role = Role.Button) { nav.push(Route.AppointmentEdit(null, null, null, selected.toString())) },
            contentAlignment = Alignment.Center,
        ) { Icon(LomaIcons.Mais, contentDescription = null, tint = LomaPalette.Mint, modifier = Modifier.size(28.dp)) }
    }
}

@Composable
private fun MiniMonth(monthStart: LocalDate, today: LocalDate, selected: LocalDate, busyDays: Set<Int>, onPick: (LocalDate) -> Unit) {
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val header = stringArrayResource(Res.array.weekdays_header)
    val length = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
    val offset = monthStart.dayOfWeek.ordinal.let { (it + 1) % 7 }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            header.forEach { Text(it, Modifier.weight(1f), style = type.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = colors.textSecondary, textAlign = TextAlign.Center) }
        }
        val cells = List(offset) { null } + (1..length).map { it }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val d = week.getOrNull(i)
                    Box(Modifier.weight(1f).height(32.dp), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val date = LocalDate(monthStart.year, monthStart.month, d)
                            val isToday = date == today
                            val isSelected = date == selected && !isToday
                            val busy = d in busyDays
                            Column(
                                Modifier.widthIn(min = 36.dp).heightIn(min = 32.dp).testTag("day_$d").clickable(role = Role.Button) { onPick(date) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                when {
                                    isToday || isSelected -> Text(
                                        d.toString(),
                                        Modifier.widthIn(min = 36.dp).height(26.dp).background(if (isToday) LomaPalette.Mint else LomaPalette.Navy, RoundedCornerShape(13.dp)).padding(top = 3.dp),
                                        style = type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                                        color = if (isToday) LomaPalette.OnMint else Color.White,
                                        textAlign = TextAlign.Center,
                                    )
                                    busy -> {
                                        Text(d.toString(), style = type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp), color = colors.textPrimary)
                                        Box(Modifier.size(5.dp).background(DotPurple, RoundedCornerShape(3.dp)))
                                    }
                                    else -> Text(d.toString(), Modifier.padding(bottom = 5.dp), style = type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp), color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String, tag: String, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val colors = LocalLomaColors.current
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth().testTag(tag).semantics(mergeDescendants = true) {}
            .background(colors.surface, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(30.dp).background(LomaPalette.AlertSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("!", style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFB3321E))
        }
        Text(text, Modifier.weight(1f), style = LocalLomaTypography.current.body.copy(fontSize = 13.sp, lineHeight = 17.sp), color = colors.textPrimary)
        trailing()
    }
}

@Composable
private fun SmallButton(text: String, bg: Color, fg: Color, border: Color?, tag: String, onClick: () -> Unit) {
    Text(
        text,
        Modifier.heightIn(min = 36.dp).testTag(tag).background(bg, RoundedCornerShape(18.dp))
            .let { if (border != null) it.border(1.5.dp, border, RoundedCornerShape(18.dp)) else it }
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        style = LocalLomaTypography.current.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
        color = fg,
    )
}

/** Cartão do atendimento: marinho se agendado, menta se confirmado, claro se concluído. */
@Composable
private fun AppointmentCard(a: Appointment, clientName: String, zone: kotlinx.datetime.TimeZone, onOpen: () -> Unit, onWhatsApp: () -> Unit, onConfirm: () -> Unit) {
    val type = LocalLomaTypography.current
    val colors = LocalLomaColors.current
    val (bg, fg, sub) = when (a.status) {
        AppointmentStatus.AGENDADO -> Triple(LomaPalette.Navy, Color.White, MutedOnNavy)
        AppointmentStatus.CONFIRMADO -> Triple(LomaPalette.Mint, LomaPalette.OnMint, LomaPalette.OnMint)
        else -> Triple(colors.surface, colors.textPrimary, colors.textSecondary)
    }
    val s = a.start.toLocalDateTime(zone)
    val minutes = ((a.end - a.start).inWholeMinutes).toInt()
    val services = a.services.joinToString(" + ") { it.name }.lowercase().replaceFirstChar { it.uppercase() }
    Column(
        Modifier.fillMaxWidth().testTag("card_${a.id}").background(bg, RoundedCornerShape(26.dp)).clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(clientName, style = type.cardName.copy(lineHeight = 27.sp), color = fg)
                val line = when (a.status) {
                    AppointmentStatus.CONFIRMADO -> stringResource(Res.string.card_confirmed, services)
                    AppointmentStatus.CONCLUIDO -> stringResource(Res.string.card_done, (a.finalValue ?: a.expected).formatBrl(showZeroCents = false))
                    else -> services
                }
                Text(line, style = type.body.copy(fontSize = 14.sp), color = sub)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(hhmm(s.hour, s.minute), style = type.body.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = fg)
                Text("${formatDuration(minutes)} · ${a.expected.formatBrl(showZeroCents = false)}", style = type.body.copy(fontSize = 13.sp), color = sub)
            }
        }
        if (a.status == AppointmentStatus.AGENDADO) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.card_not_confirmed),
                    Modifier.heightIn(min = 28.dp).border(1.dp, Color(0xFF8F8CD6), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    style = type.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                SmallButton(stringResource(Res.string.client_whatsapp), LomaPalette.Mint, LomaPalette.OnMint, null, "card_whatsapp_${a.id}", onWhatsApp)
                SmallButton(stringResource(Res.string.action_confirmed), Color.Transparent, Color.White, Color.White, "card_confirm_${a.id}", onConfirm)
            }
        }
    }
}
