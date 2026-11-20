// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.data.SettingKeys
import app.loma.platform.AuthKind
import app.loma.ui.LocalLomaGraph
import app.loma.ui.clients.ClientAvatar
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.biometric_prompt_title
import app.loma.ui.generated.resources.forgot_prompt_subtitle
import app.loma.ui.generated.resources.forgot_prompt_title
import app.loma.ui.generated.resources.lock_forgot
import app.loma.ui.generated.resources.lock_hint
import app.loma.ui.generated.resources.lock_no_device_lock
import app.loma.ui.generated.resources.lock_use_biometric
import app.loma.ui.generated.resources.lock_use_pin
import app.loma.ui.generated.resources.lock_wait
import app.loma.ui.generated.resources.lock_welcome
import app.loma.ui.generated.resources.lock_wrong
import app.loma.ui.generated.resources.lock_wrong_one
import app.loma.ui.shell.LomaIcons
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * "Bem-vinda de volta" (tela aprovada T-Bloqueio). [autoPrompt]: abrir a digital sozinha
 * (depois da abertura animada).
 */
@Composable
fun LockScreen(autoPrompt: Boolean) {
    val graph = LocalLomaGraph.current
    val lock = graph.lock
    val auth = graph.deviceAuth
    val state by lock.state.collectAsState()
    val scope = rememberCoroutineScope()
    var resetting by remember { mutableStateOf(false) }
    if (resetting) {
        // "Esqueci o PIN" confirmado pelo bloqueio do celular: cria outro e entra.
        PinSetup(onDone = { lock.setPin(it) }, onCancel = { resetting = false })
        return
    }

    val name by remember { graph.repository.setting(SettingKeys.PROFILE_NAME) }.collectAsState(initial = null)
    val photo by remember { graph.repository.setting(SettingKeys.PROFILE_PHOTO) }.collectAsState(initial = null)
    var entered by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var wait by remember { mutableIntStateOf(lock.waitSeconds()) }
    LaunchedEffect(wait > 0) {
        while (wait > 0) {
            delay(1_000)
            wait = lock.waitSeconds()
        }
    }

    val bioTitle = stringResource(Res.string.biometric_prompt_title)
    val usePin = stringResource(Res.string.lock_use_pin)
    val forgotTitle = stringResource(Res.string.forgot_prompt_title)
    val forgotSubtitle = stringResource(Res.string.forgot_prompt_subtitle)
    val noDeviceLock = stringResource(Res.string.lock_no_device_lock)
    val canBiometric = state.biometric && remember { auth.biometricAvailable() }

    suspend fun biometric() {
        if (auth.authenticate(AuthKind.BIOMETRIC, bioTitle, "", usePin)) lock.unlock()
    }
    LaunchedEffect(autoPrompt) { if (autoPrompt && canBiometric) biometric() }

    val welcome = stringResource(Res.string.lock_welcome)
    val first = name?.trim()?.takeIf { it.isNotEmpty() }
    PinScreen(
        tag = "lock_screen",
        title = if (first != null) "$welcome,\n$first" else welcome,
        titleTag = "lock_title",
        hint = stringResource(Res.string.lock_hint),
        message = if (wait > 0) stringResource(Res.string.lock_wait, wait) else message,
        messageTag = "lock_message",
        entered = entered.length,
        enabled = !busy && wait == 0,
        onDigit = { d ->
            if (entered.length < 4) {
                entered += d
                message = null
                if (entered.length == 4) {
                    val pin = entered
                    busy = true
                    scope.launch {
                        when (val r = lock.check(pin)) {
                            PinCheck.Ok -> Unit
                            is PinCheck.Wrong -> message =
                                if (r.attemptsLeft == 1) getString(Res.string.lock_wrong_one) else getString(Res.string.lock_wrong, r.attemptsLeft)
                            is PinCheck.Wait -> wait = r.seconds
                        }
                        entered = ""
                        busy = false
                    }
                }
            }
        },
        onBack = { entered = entered.dropLast(1) },
        header = { ClientAvatar(first?.take(1) ?: "L", photo?.takeIf { it.isNotBlank() }, size = 84) },
        footer = {
            val type = LocalLomaTypography.current
            Text(
                stringResource(Res.string.lock_forgot),
                Modifier.padding(bottom = 14.dp).testTag("lock_forgot").clickable(role = Role.Button) {
                    if (!auth.deviceSecure()) {
                        message = noDeviceLock
                    } else {
                        scope.launch {
                            if (auth.authenticate(AuthKind.DEVICE, forgotTitle, forgotSubtitle, usePin)) resetting = true
                        }
                    }
                }.padding(horizontal = 12.dp, vertical = 10.dp),
                style = type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = LockColors.Hint,
            )
            if (canBiometric) {
                Row(
                    Modifier.height(52.dp).testTag("lock_biometric")
                        .border(1.5.dp, LomaPalette.Mint, RoundedCornerShape(26.dp))
                        .clickable(role = Role.Button) { scope.launch { biometric() } }
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LomaIcons.Digital, contentDescription = null, tint = LomaPalette.Mint, modifier = Modifier.size(22.dp))
                    Text(stringResource(Res.string.lock_use_biometric), style = type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = LomaPalette.Mint)
                }
            }
        },
    )
}
