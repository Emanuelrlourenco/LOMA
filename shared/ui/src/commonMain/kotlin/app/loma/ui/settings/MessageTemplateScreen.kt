// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.loma.data.SettingKeys
import app.loma.salon.ConfirmationMessage
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.LomaCard
import app.loma.ui.components.LomaTextField
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.PillButton
import app.loma.ui.components.PillKind
import app.loma.ui.components.SectionLabel
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.message_fields
import app.loma.ui.generated.resources.message_preview
import app.loma.ui.generated.resources.message_restore
import app.loma.ui.generated.resources.message_title
import app.loma.ui.generated.resources.sample_client
import app.loma.ui.generated.resources.sample_service_1
import app.loma.ui.generated.resources.sample_service_2
import app.loma.ui.generated.resources.save
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaShapes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private val Fields = listOf("{nome}", "{quando}", "{hora}", "{servicos}")

/** Editor do modelo da mensagem de confirmação, com prévia ao vivo. */
@Composable
fun MessageTemplateScreen() {
    val graph = LocalLomaGraph.current
    val repo = graph.repository
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    var template by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!loaded) {
            template = repo.setting(SettingKeys.WHATSAPP_TEMPLATE).first() ?: ConfirmationMessage.DEFAULT_TEMPLATE
            loaded = true
        }
    }
    val zone = graph.zone()
    val today = graph.clock.now().toLocalDateTime(zone).date
    val preview = ConfirmationMessage.render(
        template,
        stringResource(Res.string.sample_client),
        today.atTime(13, 0).toInstant(zone),
        zone,
        today,
        listOf(stringResource(Res.string.sample_service_1), stringResource(Res.string.sample_service_2)),
        whenLabels(),
    )
    Column(Modifier.fillMaxSize()) {
        LomaTopBar(stringResource(Res.string.message_title), onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LomaTextField(template, { template = it }, stringResource(Res.string.message_title), testTag = "template", singleLine = false, minLines = 4)
            SectionLabel(stringResource(Res.string.message_fields))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Fields.forEach { field ->
                    Text(
                        field,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("field_$field")
                            .background(colors.container, LomaShapes.Pill)
                            .clickable { template = if (template.isEmpty() || template.endsWith(" ")) template + field else "$template $field" }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                }
            }
            SectionLabel(stringResource(Res.string.message_preview))
            LomaCard {
                Text(preview, style = type.body, color = colors.textPrimary, modifier = Modifier.testTag("preview"))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(stringResource(Res.string.message_restore), onClick = { template = ConfirmationMessage.DEFAULT_TEMPLATE }, kind = PillKind.Secondary, testTag = "restore_default", modifier = Modifier.weight(1f))
                PillButton(
                    stringResource(Res.string.save),
                    onClick = { scope.launch { repo.putSetting(SettingKeys.WHATSAPP_TEMPLATE, template); nav.pop() } },
                    enabled = template.isNotBlank(),
                    testTag = "save",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
