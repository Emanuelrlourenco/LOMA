// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.salon.Appointment
import app.loma.salon.AppointmentRules
import app.loma.salon.BookedService
import app.loma.salon.Money
import app.loma.salon.Service
import app.loma.salon.formatBrl
import app.loma.ui.LocalLomaGraph
import app.loma.ui.clients.filterClients
import app.loma.ui.components.LomaCard
import app.loma.ui.components.LomaTextField
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.MoneyField
import app.loma.ui.components.PillButton
import app.loma.ui.components.PillKind
import app.loma.ui.components.SectionLabel
import app.loma.ui.components.formatDuration
import app.loma.ui.components.hhmm
import app.loma.ui.components.parseTime
import app.loma.ui.components.toInput
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.appt_book
import app.loma.ui.generated.resources.appt_choose_client
import app.loma.ui.generated.resources.appt_choose_time
import app.loma.ui.generated.resources.appt_client
import app.loma.ui.generated.resources.appt_conflict
import app.loma.ui.generated.resources.appt_date
import app.loma.ui.generated.resources.appt_edit_title
import app.loma.ui.generated.resources.appt_new_client
import app.loma.ui.generated.resources.appt_new_title
import app.loma.ui.generated.resources.appt_no_services
import app.loma.ui.generated.resources.appt_note
import app.loma.ui.generated.resources.appt_repeat
import app.loma.ui.generated.resources.appt_repeat_times
import app.loma.ui.generated.resources.appt_search_client
import app.loma.ui.generated.resources.appt_services
import app.loma.ui.generated.resources.appt_time
import app.loma.ui.generated.resources.appt_value
import app.loma.ui.generated.resources.cancel
import app.loma.ui.generated.resources.ok
import app.loma.ui.generated.resources.save
import app.loma.ui.generated.resources.time_invalid
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaShapes
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private fun idsToText(ids: List<Long>) = ids.joinToString(",")
private fun textToIds(text: String) = text.split(',').mapNotNull { it.toLongOrNull() }

/** Agendar (ou editar) um atendimento: cliente, serviços, data e hora, valor, anotação e repetição. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentEditScreen(id: Long?, clientId: Long?, rescheduleOf: Long?, day: String? = null) {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val zone = graph.zone()
    val today = graph.clock.now().toLocalDateTime(zone).date

    var chosenClient by rememberSaveable { mutableStateOf(clientId) }
    var serviceIds by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(day ?: today.toString()) }
    var timeText by rememberSaveable { mutableStateOf("") }
    var valueText by rememberSaveable { mutableStateOf("") }
    var valueEdited by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var repeatTimes by rememberSaveable { mutableStateOf("4") }
    var loaded by rememberSaveable { mutableStateOf(id == null && rescheduleOf == null) }
    var existing by remember { mutableStateOf<Appointment?>(null) }
    var pickClient by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val services by remember { repo.services() }.collectAsState(initial = emptyList())
    val clients by remember { repo.clients() }.collectAsState(initial = emptyList())

    // Carrega o atendimento a editar ou o original a remarcar.
    LaunchedEffect(id, rescheduleOf) {
        val sourceId = id ?: rescheduleOf ?: return@LaunchedEffect
        val a = repo.appointment(sourceId).first() ?: return@LaunchedEffect
        if (id != null) existing = a
        if (!loaded) {
            chosenClient = a.clientId
            serviceIds = idsToText(a.services.mapNotNull { it.serviceId })
            note = a.note
            if (id != null) {
                val local = a.start.toLocalDateTime(TimeZone.of(a.zoneId))
                dateText = local.date.toString()
                timeText = hhmm(local.hour, local.minute)
                valueText = a.expected.toInput()
                valueEdited = true
            }
            loaded = true
        }
    }
    // Cliente recém-cadastrada pelo "+ Nova cliente".
    LaunchedEffect(nav.clientResult) {
        nav.clientResult?.let {
            chosenClient = it
            nav.clientResult = null
        }
    }

    val selected: List<Service> = textToIds(serviceIds).mapNotNull { sid -> services.firstOrNull { it.id == sid } }
    // Serviços do atendimento em edição que foram desativados continuam valendo.
    val keptBooked: List<BookedService> = existing?.services?.filter { b -> b.serviceId == null || services.none { it.id == b.serviceId } }.orEmpty()
    val totalMinutes = selected.sumOf { it.durationMinutes } + keptBooked.sumOf { it.durationMinutes }
    val totalPrice = selected.fold(Money.ZERO) { acc, s -> acc + s.price } + keptBooked.fold(Money.ZERO) { acc, s -> acc + s.price }
    LaunchedEffect(totalPrice, valueEdited) { if (!valueEdited) valueText = if (totalPrice == Money.ZERO) "" else totalPrice.toInput() }

    val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: today
    val time = parseTime(timeText)
    val start: Instant? = time?.let { (h, m) -> LocalDateTime(date, LocalTime(h, m)).toInstant(zone) }
    val value = Money.parseBrl(valueText)
    val client = clients.firstOrNull { it.id == chosenClient }

    val dayAppointments by remember(dateText) { repo.appointmentsOn(date, zone) }.collectAsState(initial = emptyList())
    val conflict = start?.let { s ->
        val candidate = Appointment(id = id ?: 0, clientId = chosenClient, start = s, end = s + totalMinutes.minutes, zoneId = zone.id,
            status = app.loma.salon.AppointmentStatus.AGENDADO, expected = Money.ZERO, services = emptyList())
        if (totalMinutes == 0) null else AppointmentRules.conflicts(candidate, dayAppointments).firstOrNull()
    }
    val times = repeatTimes.toIntOrNull()
    val valid = chosenClient != null && (selected.isNotEmpty() || keptBooked.isNotEmpty()) && start != null &&
        value != null && value >= Money.ZERO && (!repeat || (times != null && times in 2..12))

    Column(Modifier.fillMaxSize()) {
        LomaTopBar(stringResource(if (id == null) Res.string.appt_new_title else Res.string.appt_edit_title), onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(Res.string.appt_client))
            LomaCard(onClick = { pickClient = true }, modifier = Modifier.testTag("choose_client")) {
                Text(client?.name ?: stringResource(Res.string.appt_choose_client), style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = if (client != null) colors.textPrimary else colors.textSecondary)
            }

            SectionLabel(stringResource(Res.string.appt_services))
            if (services.isEmpty()) Text(stringResource(Res.string.appt_no_services), style = type.body, color = colors.textSecondary)
            services.forEach { s ->
                val on = s.id in textToIds(serviceIds)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("svc_${s.id}")
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Checkbox) {
                            val ids = textToIds(serviceIds)
                            serviceIds = idsToText(if (on) ids - s.id else ids + s.id)
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = on, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = colors.primary))
                    Spacer(Modifier.width(8.dp))
                    Text(s.name, Modifier.weight(1f), style = type.body.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Text("${formatDuration(s.durationMinutes)} · ${s.price.formatBrl(showZeroCents = false)}", style = type.body.copy(fontSize = 14.sp), color = colors.textSecondary)
                }
            }

            SectionLabel(stringResource(Res.string.appt_date))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dayLabel(date),
                    Modifier.weight(1f).heightIn(min = 52.dp).testTag("date").background(colors.surface, RoundedCornerShape(16.dp))
                        .clickable { pickDate = true }.padding(horizontal = 16.dp, vertical = 15.dp),
                    style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = colors.textPrimary,
                )
                LomaTextField(timeText, { timeText = it.take(5) }, stringResource(Res.string.appt_time), Modifier.weight(1f), "start_time", KeyboardType.Number,
                    error = if (timeText.isNotBlank() && time == null) stringResource(Res.string.time_invalid) else null)
            }
            TextButton(onClick = { pickTime = true }) { Text(stringResource(Res.string.appt_choose_time), color = colors.primary) }
            if (start != null && totalMinutes > 0) {
                val end = (start + totalMinutes.minutes).toLocalDateTime(zone)
                Text(
                    "${hhmm(time!!.first, time.second)} – ${hhmm(end.hour, end.minute)} · ${formatDuration(totalMinutes)}",
                    Modifier.testTag("time_range"),
                    style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
                    color = colors.textPrimary,
                )
            }
            MoneyField(valueText, { valueText = it; valueEdited = true }, stringResource(Res.string.appt_value), testTag = "appt_value")
            LomaTextField(note, { note = it }, stringResource(Res.string.appt_note), testTag = "appt_note", singleLine = false)

            if (id == null) {
                Row(Modifier.fillMaxWidth().testTag("repeat").clickable { repeat = !repeat }.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.appt_repeat), Modifier.weight(1f), style = type.body.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Switch(checked = repeat, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent))
                }
                if (repeat) {
                    LomaTextField(repeatTimes, { repeatTimes = it.filter(Char::isDigit).take(2) }, stringResource(Res.string.appt_repeat_times), testTag = "repeat_times", keyboardType = KeyboardType.Number)
                }
            }
            if (conflict != null) {
                val who = clients.firstOrNull { it.id == conflict.clientId }?.name.orEmpty()
                val at = conflict.start.toLocalDateTime(zone)
                Text(
                    stringResource(Res.string.appt_conflict, "${hhmm(at.hour, at.minute)} $who".trim()),
                    Modifier.fillMaxWidth().testTag("conflict_warning").background(app.loma.ui.theme.LomaPalette.AlertSoft, RoundedCornerShape(18.dp)).padding(14.dp),
                    style = type.body.copy(fontSize = 14.sp),
                    color = app.loma.ui.theme.LomaPalette.OnAlertSoft,
                )
            }
            Spacer(Modifier.height(4.dp))
            PillButton(
                stringResource(if (id == null) Res.string.appt_book else Res.string.save),
                enabled = valid,
                testTag = "save",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val s = start ?: return@PillButton
                    val v = value ?: return@PillButton
                    scope.launch {
                        val old = existing
                        if (old == null) {
                            val booked = AppointmentRules.book(chosenClient!!, selected, s, zone).copy(expected = v, note = note.trim())
                            val newId = repo.save(booked)
                            if (repeat && times != null) repo.saveAll(AppointmentRules.repeatWeekly(booked.copy(id = newId), times - 1))
                            nav.replaceTop(Route.AppointmentDetail(newId))
                        } else {
                            val rebooked = AppointmentRules.book(chosenClient!!, selected, s, zone)
                            val all = keptBooked + rebooked.services
                            repo.save(old.copy(clientId = chosenClient, start = s, end = s + all.sumOf { it.durationMinutes }.minutes,
                                zoneId = zone.id, services = all, expected = v, note = note.trim()))
                            nav.pop()
                        }
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickClient) {
        ClientChooser(
            clients = clients,
            onPick = { chosenClient = it; pickClient = false },
            onNew = { pickClient = false; nav.push(Route.ClientEdit(null)) },
            onDismiss = { pickClient = false },
        )
    }
    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateText = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString() }
                    pickDate = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val state = rememberTimePickerState(initialHour = time?.first ?: 9, initialMinute = time?.second ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = { TextButton(onClick = { timeText = hhmm(state.hour, state.minute); pickTime = false }) { Text(stringResource(Res.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(Res.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

/** Lista de clientes com busca e "+ Nova cliente". */
@Composable
private fun ClientChooser(clients: List<app.loma.salon.Client>, onPick: (Long) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onNew, modifier = Modifier.testTag("chooser_new_client")) { Text(stringResource(Res.string.appt_new_client)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        title = { Text(stringResource(Res.string.appt_choose_client)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LomaTextField(q, { q = it }, stringResource(Res.string.appt_search_client), testTag = "chooser_search")
                filterClients(clients, q).forEach { c ->
                    Text(
                        c.name,
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chooser_${c.id}").clickable { onPick(c.id) }.padding(vertical = 12.dp),
                        style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = colors.textPrimary,
                    )
                }
            }
        },
    )
}
