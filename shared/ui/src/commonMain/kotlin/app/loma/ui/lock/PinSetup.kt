// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.pin_setup_confirm
import app.loma.ui.generated.resources.pin_setup_mismatch
import app.loma.ui.generated.resources.pin_setup_new
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Criar um PIN: digita e confirma. O PIN fica só em memória (não vai para o estado salvo da tela).
 * [hint] aparece na primeira etapa (ex.: o aviso sobre esquecer o PIN).
 */
@Composable
fun PinSetup(onDone: suspend (String) -> Unit, onCancel: (() -> Unit)?, hint: String? = null) {
    var first by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val mismatch = stringResource(Res.string.pin_setup_mismatch)
    PinScreen(
        tag = "pin_setup",
        title = stringResource(if (first == null) Res.string.pin_setup_new else Res.string.pin_setup_confirm),
        titleTag = "pin_setup_title",
        hint = if (first == null) hint else null,
        message = error,
        messageTag = "pin_setup_error",
        entered = entered.length,
        enabled = !busy,
        onDigit = { d ->
            if (entered.length < 4) {
                entered += d
                error = null
                if (entered.length == 4) {
                    val pin = entered
                    entered = ""
                    val f = first
                    when {
                        f == null -> first = pin
                        f == pin -> {
                            busy = true
                            scope.launch { onDone(pin) }
                        }
                        else -> {
                            first = null
                            error = mismatch
                        }
                    }
                }
            }
        },
        onBack = { entered = entered.dropLast(1) },
        onCancel = onCancel,
    )
}
