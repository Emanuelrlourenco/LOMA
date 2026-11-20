// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.appt_updated
import app.loma.ui.generated.resources.undo
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import app.loma.ui.theme.LomaShapes
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** "Atendimento atualizado · Desfazer" por 5 s. [key] muda a cada nova mudança. */
@Composable
fun UndoSnackbar(key: Any, onUndo: () -> Unit, onTimeout: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(key) {
        delay(5_000)
        onTimeout()
    }
    val type = LocalLomaTypography.current
    Row(
        modifier.fillMaxWidth().padding(16.dp).background(LomaPalette.Navy, LomaShapes.Item).heightIn(min = 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.appt_updated), Modifier.weight(1f), style = type.body, color = androidx.compose.ui.graphics.Color.White)
        Text(
            stringResource(Res.string.undo),
            Modifier.testTag("undo").clickable(onClick = onUndo).padding(12.dp),
            style = type.body.copy(fontWeight = FontWeight.ExtraBold),
            color = LomaPalette.Mint,
        )
    }
}
