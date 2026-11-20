// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.loma.ui.LocalLomaGraph
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.lock_wait
import app.loma.ui.generated.resources.lock_wrong
import app.loma.ui.generated.resources.lock_wrong_one
import app.loma.ui.generated.resources.pin_verify_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Pede o PIN atual antes de trocar ou desligar o bloqueio (mesmas regras de tentativas). */
@Composable
fun PinVerify(onVerified: suspend () -> Unit, onCancel: () -> Unit) {
    val lock = LocalLomaGraph.current.lock
    var entered by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PinScreen(
        tag = "pin_verify",
        title = stringResource(Res.string.pin_verify_title),
        titleTag = "pin_verify_title",
        hint = null,
        message = message,
        messageTag = "pin_verify_error",
        entered = entered.length,
        enabled = !busy,
        onDigit = { d ->
            if (entered.length < 4) {
                entered += d
                message = null
                if (entered.length == 4) {
                    val pin = entered
                    busy = true
                    scope.launch {
                        when (val r = lock.check(pin)) {
                            PinCheck.Ok -> onVerified()
                            is PinCheck.Wrong -> message =
                                if (r.attemptsLeft == 1) getString(Res.string.lock_wrong_one) else getString(Res.string.lock_wrong, r.attemptsLeft)
                            is PinCheck.Wait -> message = getString(Res.string.lock_wait, r.seconds)
                        }
                        entered = ""
                        busy = false
                    }
                }
            }
        },
        onBack = { entered = entered.dropLast(1) },
        onCancel = onCancel,
    )
}
