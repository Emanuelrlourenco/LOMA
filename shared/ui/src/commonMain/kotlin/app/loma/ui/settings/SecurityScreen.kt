// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.platform.AuthKind
import app.loma.ui.LocalLomaGraph
import app.loma.ui.components.LomaCard
import app.loma.ui.components.LomaTopBar
import app.loma.ui.components.MenuRow
import app.loma.ui.components.SectionLabel
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.biometric_prompt_title
import app.loma.ui.generated.resources.lock_use_pin
import app.loma.ui.generated.resources.security_biometric
import app.loma.ui.generated.resources.security_change_pin
import app.loma.ui.generated.resources.security_change_pin_hint
import app.loma.ui.generated.resources.security_pin
import app.loma.ui.generated.resources.security_pin_hint
import app.loma.ui.generated.resources.security_timeout
import app.loma.ui.generated.resources.security_timeout_0
import app.loma.ui.generated.resources.security_timeout_300
import app.loma.ui.generated.resources.security_timeout_60
import app.loma.ui.generated.resources.security_title
import app.loma.ui.generated.resources.security_warning_ok
import app.loma.ui.generated.resources.security_warning_unsafe
import app.loma.ui.lock.PinSetup
import app.loma.ui.lock.PinVerify
import app.loma.ui.shell.LocalNavigator
import app.loma.ui.shell.Route
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Ajustes › Segurança: PIN, digital e depois de quanto tempo bloquear (design §7.12a). */
@Composable
fun SecurityScreen() {
    val graph = LocalLomaGraph.current
    val lock = graph.lock
    val auth = graph.deviceAuth
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val state by lock.state.collectAsState()
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val bioTitle = stringResource(Res.string.biometric_prompt_title)
    val usePin = stringResource(Res.string.lock_use_pin)
    val hasBiometric = remember { auth.biometricAvailable() }
    val deviceSecure = remember { auth.deviceSecure() }
    val switchColors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent)

    Column(Modifier.fillMaxSize()) {
        LomaTopBar(stringResource(Res.string.security_title), onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            LomaCard {
                SwitchRow(stringResource(Res.string.security_pin), stringResource(Res.string.security_pin_hint)) {
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { on -> nav.push(Route.PinChange(if (on) "new" else "disable")) },
                        modifier = Modifier.testTag("security_pin_switch"),
                        colors = switchColors,
                    )
                }
                if (state.enabled && hasBiometric) {
                    SwitchRow(stringResource(Res.string.security_biometric), null) {
                        Switch(
                            checked = state.biometric,
                            onCheckedChange = { on ->
                                // Ligar pede a digital uma vez, para confirmar que funciona.
                                if (!on) lock.setBiometric(false)
                                else scope.launch { if (auth.authenticate(AuthKind.BIOMETRIC, bioTitle, "", usePin)) lock.setBiometric(true) }
                            },
                            modifier = Modifier.testTag("security_biometric_switch"),
                            colors = switchColors,
                        )
                    }
                }
            }
            if (state.enabled) {
                SectionLabel(stringResource(Res.string.security_timeout))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to Res.string.security_timeout_0, 60 to Res.string.security_timeout_60, 300 to Res.string.security_timeout_300).forEach { (s, label) ->
                        TimeoutChip(label, selected = state.timeoutSeconds == s, tag = "security_timeout_$s", modifier = Modifier.weight(1f)) { lock.setTimeout(s) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                LomaCard {
                    MenuRow(stringResource(Res.string.security_change_pin), stringResource(Res.string.security_change_pin_hint), { nav.push(Route.PinChange("change")) }, "security_change_pin")
                }
            }
            Text(
                stringResource(if (deviceSecure) Res.string.security_warning_ok else Res.string.security_warning_unsafe),
                Modifier.padding(top = 16.dp, bottom = 24.dp).testTag("security_warning"),
                style = type.body.copy(fontSize = 14.sp),
                color = if (deviceSecure) colors.textSecondary else colors.textPrimary,
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, switch: @Composable () -> Unit) {
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp), color = colors.textPrimary)
            if (hint != null) Text(hint, style = type.body.copy(fontSize = 13.sp), color = colors.textSecondary)
        }
        switch()
    }
}

@Composable
private fun TimeoutChip(label: StringResource, selected: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    val shape = RoundedCornerShape(20.dp)
    Text(
        stringResource(label),
        modifier.heightIn(min = 44.dp).testTag(tag)
            .background(if (selected) LomaPalette.Navy else Color.Transparent, shape)
            .border(1.5.dp, if (selected) LomaPalette.Navy else colors.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 11.dp),
        style = type.body.copy(fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
        color = if (selected) Color.White else colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}

/** Criar ("new"), trocar ("change") ou desligar ("disable") o PIN. */
@Composable
fun PinChangeScreen(mode: String) {
    val graph = LocalLomaGraph.current
    val lock = graph.lock
    val nav = LocalNavigator.current
    val warning = stringResource(if (graph.deviceAuth.deviceSecure()) Res.string.security_warning_ok else Res.string.security_warning_unsafe)
    var verified by remember { mutableStateOf(mode == "new") }
    when {
        !verified -> PinVerify(
            onVerified = {
                if (mode == "disable") {
                    lock.disable()
                    nav.pop()
                } else {
                    verified = true
                }
            },
            onCancel = { nav.pop() },
        )
        else -> PinSetup(
            onDone = {
                lock.setPin(it)
                nav.pop()
            },
            onCancel = { nav.pop() },
            hint = warning,
        )
    }
}
