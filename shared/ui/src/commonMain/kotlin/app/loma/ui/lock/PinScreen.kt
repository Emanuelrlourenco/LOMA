// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.back
import app.loma.ui.shell.LomaIcons
import app.loma.ui.theme.LocalLomaTypography
import org.jetbrains.compose.resources.stringResource

/** Cor das mensagens de erro sobre o fundo azul-marinho. */
internal val LockError = Color(0xFFFFB4A8)

/** Esqueleto azul-marinho de T-Bloqueio: cabeçalho, título, dica, pontos, teclado e rodapé. */
@Composable
internal fun PinScreen(
    tag: String,
    title: String,
    titleTag: String,
    hint: String?,
    message: String?,
    messageTag: String,
    entered: Int,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBack: () -> Unit,
    onCancel: (() -> Unit)? = null,
    header: @Composable () -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val type = LocalLomaTypography.current
    // O fundo recebe os toques (sem consumir, para não cancelar os botões): o que está por trás
    // do bloqueio deixa de ser atingido.
    Box(Modifier.fillMaxSize().testTag(tag).background(LockColors.Background).pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent() }
    }) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 60.dp, y = (-60).dp).size(220.dp).background(LockColors.Key, CircleShape))
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header()
            Text(
                title,
                Modifier.padding(top = 20.dp).testTag(titleTag).semantics { heading() },
                style = type.body.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (hint != null) {
                Text(hint, Modifier.padding(top = 8.dp), style = type.body.copy(fontSize = 15.sp), color = LockColors.Hint, textAlign = TextAlign.Center)
            }
            Text(
                message.orEmpty(),
                Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 20.dp).testTag(messageTag).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = LockError,
                textAlign = TextAlign.Center,
            )
            PinPad(length = 4, entered = entered, enabled = enabled, onDigit = onDigit, onBack = onBack)
            Spacer(Modifier.weight(1f))
            footer()
        }
        if (onCancel != null) {
            val label = stringResource(Res.string.back)
            Box(
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp).size(48.dp).testTag("pin_cancel")
                    .clickable(role = Role.Button, onClick = onCancel).semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) { Icon(LomaIcons.Voltar, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        }
    }
}
