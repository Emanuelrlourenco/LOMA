// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.salon.formatBrl
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.Dot
import app.loma.ui.components.LomaCard
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.PillButton
import app.loma.ui.components.formatDuration
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.move_down
import app.loma.ui.generated.resources.move_up
import app.loma.ui.generated.resources.service_active
import app.loma.ui.generated.resources.services_empty
import app.loma.ui.generated.resources.services_new
import app.loma.ui.generated.resources.services_title
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.LomaIcons
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Lista de serviços: reordenar, ativar/desativar e criar. */
@Composable
fun ServicesScreen() {
    val repo = LocalLomaGraph.current.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val flow = remember { repo.services(includeInactive = true) }
    val services by flow.collectAsState(initial = emptyList())
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LomaTopBar(stringResource(Res.string.services_title), onBack = { nav.pop() })
            if (services.isEmpty()) {
                Text(stringResource(Res.string.services_empty), modifier = Modifier.padding(22.dp), style = type.body, color = colors.textSecondary)
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(services, key = { _, s -> s.id }) { index, s ->
                    LomaCard(onClick = { nav.push(Route.ServiceEdit(s.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(Color(s.colorArgb))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp), color = if (s.active) colors.textPrimary else colors.textSecondary)
                                Text("${formatDuration(s.durationMinutes)} · ${s.price.formatBrl(showZeroCents = false)}", style = type.body, color = colors.textSecondary)
                            }
                            IconButton(onClick = { scope.launch { repo.moveService(s.id, up = true) } }, enabled = index > 0, modifier = Modifier.testTag("up_${s.id}")) {
                                Icon(LomaIcons.Subir, contentDescription = stringResource(Res.string.move_up), tint = colors.textSecondary)
                            }
                            IconButton(onClick = { scope.launch { repo.moveService(s.id, up = false) } }, enabled = index < services.lastIndex, modifier = Modifier.testTag("down_${s.id}")) {
                                Icon(LomaIcons.Descer, contentDescription = stringResource(Res.string.move_down), tint = colors.textSecondary)
                            }
                            Switch(
                                checked = s.active,
                                onCheckedChange = { checked -> scope.launch { repo.saveService(s.copy(active = checked)) } },
                                modifier = Modifier.testTag("active_${s.id}"),
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
                            )
                        }
                        if (!s.active) Text(stringResource(Res.string.service_active) + ": —", style = type.body.copy(fontSize = 12.sp), color = colors.textSecondary)
                    }
                }
            }
        }
        PillButton(
            stringResource(Res.string.services_new),
            onClick = { nav.push(Route.ServiceEdit(null)) },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(18.dp).fillMaxWidth(),
            testTag = "new_service",
        )
    }
}
