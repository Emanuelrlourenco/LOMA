// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.loma.salon.Money
import app.loma.salon.Service
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.Dot
import app.loma.ui.components.LomaTextField
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.MoneyField
import app.loma.ui.components.PillButton
import app.loma.ui.components.SectionLabel
import app.loma.ui.components.toInput
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.save
import app.loma.ui.generated.resources.service_color
import app.loma.ui.generated.resources.service_duration
import app.loma.ui.generated.resources.service_edit_title
import app.loma.ui.generated.resources.service_name
import app.loma.ui.generated.resources.service_new_title
import app.loma.ui.generated.resources.service_price
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LomaShapes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Cores para os serviços (paleta da marca). */
val ServiceColors: List<Long> = listOf(
    0xFF5ED6B4, 0xFFB69CF2, 0xFFFF7A66, 0xFF2A2985, 0xFFF5C451, 0xFF6FA8FF, 0xFFF29CC3, 0xFF8BD17C,
)

/** Criar ou editar um serviço. */
@Composable
fun ServiceEditScreen(id: Long?) {
    val repo = LocalLomaGraph.current.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    var name by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("60") }
    var price by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableLongStateOf(ServiceColors[0]) }
    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var existing by remember { mutableStateOf<Service?>(null) }
    LaunchedEffect(id) {
        if (id != null) {
            val s = repo.services(includeInactive = true).first().firstOrNull { it.id == id }
            existing = s
            if (!loaded && s != null) {
                name = s.name
                duration = s.durationMinutes.toString()
                price = s.price.toInput()
                color = s.colorArgb
                loaded = true
            }
        }
    }
    val minutes = duration.trim().toIntOrNull()
    val money = Money.parseBrl(price)
    val valid = name.isNotBlank() && minutes != null && minutes > 0 && money != null && money >= Money.ZERO
    Column(Modifier.fillMaxSize()) {
        LomaTopBar(stringResource(if (id == null) Res.string.service_new_title else Res.string.service_edit_title), onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LomaTextField(name, { name = it }, stringResource(Res.string.service_name), testTag = "service_name")
            LomaTextField(duration, { duration = it.filter(Char::isDigit).take(4) }, stringResource(Res.string.service_duration), testTag = "service_duration", keyboardType = KeyboardType.Number)
            MoneyField(price, { price = it }, stringResource(Res.string.service_price), testTag = "service_price")
            SectionLabel(stringResource(Res.string.service_color))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceColors.forEach { c ->
                    Box(
                        Modifier.size(36.dp)
                            .testTag("color_$c")
                            .let { if (c == color) it.border(3.dp, colors.textPrimary, LomaShapes.Pill) else it }
                            .clickable { color = c }
                            .padding(5.dp),
                    ) { Dot(Color(c), 26) }
                }
            }
            Spacer(Modifier.height(12.dp))
            PillButton(
                stringResource(Res.string.save),
                enabled = valid,
                testTag = "save",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        val order = existing?.sortOrder ?: repo.services(includeInactive = true).first().size
                        repo.saveService(
                            Service(
                                id = id ?: 0,
                                name = name.trim(),
                                durationMinutes = minutes!!,
                                price = money!!,
                                colorArgb = color,
                                active = existing?.active ?: true,
                                sortOrder = order,
                            ),
                        )
                        nav.pop()
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
