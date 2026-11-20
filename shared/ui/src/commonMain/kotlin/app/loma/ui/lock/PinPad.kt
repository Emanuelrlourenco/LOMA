// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.pin_delete
import app.loma.ui.generated.resources.pin_dots_label
import app.loma.ui.shell.LomaIcons
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaPalette
import org.jetbrains.compose.resources.stringResource

/** Cores da tela aprovada T-Bloqueio (fixas, valem nos dois temas). */
internal object LockColors {
    val Background = LomaPalette.Navy
    val Key = Color(0xFF23227A)
    val Hint = Color(0xFFC9C6F0)
    val DotOff = Color(0xFF8F8CD6)
}

/** Pontos do PIN e teclado 3×4 (T-Bloqueio). */
@Composable
fun PinPad(length: Int, entered: Int, enabled: Boolean, onDigit: (Char) -> Unit, onBack: () -> Unit) {
    val dotsLabel = stringResource(Res.string.pin_dots_label, entered, length)
    Row(
        Modifier.padding(top = 32.dp).height(18.dp).testTag("pin_dots").semantics { contentDescription = dotsLabel },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(length) { i ->
            if (i < entered) {
                Box(Modifier.size(18.dp).background(LomaPalette.Mint, CircleShape))
            } else {
                Box(Modifier.size(14.dp).border(2.dp, LockColors.DotOff, CircleShape))
            }
        }
    }
    val type = LocalLomaTypography.current
    val rows = listOf("123", "456", "789", " 0<")
    Column(Modifier.padding(top = 40.dp).width(280.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { key ->
                    val mod = Modifier.weight(1f).height(72.dp)
                    when (key) {
                        ' ' -> Spacer(mod)
                        '<' -> {
                            val label = stringResource(Res.string.pin_delete)
                            Box(
                                mod.testTag("pin_key_back").clip(RoundedCornerShape(36.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onBack)
                                    .semantics { contentDescription = label },
                                contentAlignment = Alignment.Center,
                            ) { Icon(LomaIcons.Apagar, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                        }
                        else -> Box(
                            mod.testTag("pin_key_$key").clip(RoundedCornerShape(36.dp)).background(LockColors.Key)
                                .clickable(enabled = enabled, role = Role.Button) { onDigit(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                key.toString(),
                                style = type.body.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                                color = if (enabled) Color.White else LockColors.DotOff,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
