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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.salon.AppointmentRules
import app.loma.salon.Money
import app.loma.salon.formatBrl
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.toInput
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.cancel
import app.loma.ui.generated.resources.client_removed
import app.loma.ui.generated.resources.complete_confirm
import app.loma.ui.generated.resources.complete_diff
import app.loma.ui.generated.resources.complete_expected
import app.loma.ui.generated.resources.complete_final
import app.loma.ui.generated.resources.complete_help
import app.loma.ui.generated.resources.complete_note
import app.loma.ui.generated.resources.complete_note_hint
import app.loma.ui.generated.resources.complete_paid_date
import app.loma.ui.generated.resources.complete_title
import app.loma.ui.generated.resources.complete_value_required
import app.loma.ui.generated.resources.ok
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private val Ink = LomaPalette.Navy
private val Secondary = LomaPalette.TextSecondary

/** Folha "Concluir atendimento" (T-Concluir): valor final, data do pagamento e anotação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteAppointmentScreen(id: Long) {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val type = LocalLomaTypography.current
    val zone = graph.zone()
    val appointment by remember(id) { repo.appointment(id) }.collectAsState(initial = null)
    val a = appointment
    val client by remember(a?.clientId) { a?.clientId?.let { repo.client(it) } ?: kotlinx.coroutines.flow.flowOf(null) }.collectAsState(initial = null)

    var value by rememberSaveable { mutableStateOf("") }
    var paidDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (!loaded) {
            val first = repo.appointment(id).first() ?: return@LaunchedEffect
            value = first.expected.toInput()
            paidDate = AppointmentRules.defaultPaidAt(first, graph.clock.now(), zone).toLocalDateTime(zone).date.toString()
            note = first.note
            loaded = true
        }
    }

    val money = Money.parseBrl(value)
    val valid = money != null && money >= Money.ZERO && a != null && a.isOpen
    val date = runCatching { LocalDate.parse(paidDate) }.getOrNull()
    val name = client?.name ?: stringResource(Res.string.client_removed)
    val servicesText = a?.services?.joinToString(" + ") { it.name }?.lowercase()?.replaceFirstChar { it.uppercase() }.orEmpty()

    Box(Modifier.fillMaxSize().background(Ink)) {
        // Fundo: o atendimento, apagado (como na tela aprovada).
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 22.dp, vertical = 40.dp)) {
            Text(name.substringBefore(' '), style = type.screenTitle.copy(fontSize = 44.sp), color = Color.White.copy(alpha = 0.35f))
            Text(servicesText, Modifier.padding(top = 8.dp), style = type.body.copy(fontSize = 17.sp), color = Color.White.copy(alpha = 0.35f))
        }
        Box(Modifier.fillMaxSize().background(Color(0x59080828)))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(top = 120.dp)
                .background(LomaPalette.Background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 5.dp).background(LomaPalette.Border, RoundedCornerShape(3.dp)))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Text(stringResource(Res.string.complete_title), Modifier.padding(top = 16.dp), style = type.screenTitle.copy(fontSize = 28.sp, letterSpacing = (-0.4).sp), color = Ink)
                Text("$name · $servicesText", Modifier.padding(top = 4.dp), style = type.body, color = Secondary)
                Row(
                    Modifier.padding(top = 18.dp).fillMaxWidth().background(LomaPalette.Container, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.complete_expected), Modifier.weight(1f), style = type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Secondary)
                    Text(a?.expected?.formatBrl().orEmpty(), Modifier.testTag("expected_value"), style = type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = Ink)
                }
                Label(stringResource(Res.string.complete_final))
                Row(
                    Modifier.padding(top = 6.dp).fillMaxWidth().heightIn(min = 64.dp).background(Color.White, RoundedCornerShape(20.dp))
                        .border(2.dp, Ink, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("R$", style = type.body.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = Secondary)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.weight(1f).testTag("final_value"),
                        singleLine = true,
                        textStyle = type.body.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(Ink),
                    )
                }
                val diff = if (money != null && a != null) money - a.expected else null
                Text(
                    buildAnnotatedString {
                        append(stringResource(Res.string.complete_help))
                        if (money == null || money < Money.ZERO) {
                            append(" ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LomaPalette.Coral)) { append(stringResource(Res.string.complete_value_required)) }
                        }
                    },
                    Modifier.padding(top = 6.dp),
                    style = type.body.copy(fontSize = 13.sp),
                    color = Secondary,
                )
                if (diff != null && diff != Money.ZERO) {
                    val signed = (if (diff > Money.ZERO) "+" else "−") + Money(kotlin.math.abs(diff.cents)).formatBrl()
                    Text(stringResource(Res.string.complete_diff, signed), Modifier.testTag("value_diff"), style = type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = Color(0xFF0E7A5C))
                }
                Label(stringResource(Res.string.complete_paid_date))
                Text(
                    date?.let { dayLabel(it) }.orEmpty(),
                    Modifier.padding(top = 6.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("paid_date")
                        .background(Color.White, RoundedCornerShape(18.dp)).clickable(role = Role.Button) { pickDate = true }.padding(horizontal = 16.dp, vertical = 15.dp),
                    style = type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    color = Ink,
                )
                Row(Modifier.padding(top = 16.dp)) {
                    Text(stringResource(Res.string.complete_note), style = type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
                    Text(" " + stringResource(Res.string.complete_note_hint), style = type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = Secondary)
                }
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.padding(top = 6.dp).fillMaxWidth().heightIn(min = 80.dp).testTag("complete_note")
                        .background(Color.White, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = type.body.copy(fontSize = 15.sp, lineHeight = 21.sp, color = Ink),
                    cursorBrush = SolidColor(Ink),
                )
            }
            Spacer(Modifier.height(14.dp))
            ConfirmButton(stringResource(Res.string.complete_confirm), valid) {
                val appt = a ?: return@ConfirmButton
                val m = money ?: return@ConfirmButton
                val now = graph.clock.now()
                val today = now.toLocalDateTime(zone).date
                val paidAt: Instant = when {
                    date == null || date == today -> now
                    else -> date.atTime(12, 0).toInstant(zone)
                }
                scope.launch {
                    repo.save(AppointmentRules.complete(appt, m, paidAt, note.trim()))
                    nav.pendingUndo = appt
                    nav.pop()
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("complete_cancel").clickable(role = Role.Button) { nav.pop() }, contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.cancel), style = type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Ink)
            }
        }
    }
    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: graph.clock.now().toLocalDateTime(zone).date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { paidDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString() }
                    pickDate = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(top = 16.dp), style = LocalLomaTypography.current.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink)
}

@Composable
private fun ConfirmButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("complete_confirm")
            .background(if (enabled) Ink else Ink.copy(alpha = 0.4f), RoundedCornerShape(29.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalLomaTypography.current.body.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = Color.White)
    }
}
