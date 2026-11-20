// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.salon.Money
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.back
import app.loma.ui.generated.resources.money_invalid
import app.loma.ui.shell.LomaIcons
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import app.loma.ui.theme.LomaShapes
import org.jetbrains.compose.resources.stringResource

/** Título de tela (30 sp ExtraBold) com voltar opcional. */
@Composable
fun LomaTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    val colors = LocalLomaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = if (onBack != null) 8.dp else 22.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                Icon(LomaIcons.Voltar, contentDescription = stringResource(Res.string.back), tint = colors.textPrimary)
            }
        }
        Text(
            title,
            style = LocalLomaTypography.current.screenTitle,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).testTag("screen_title").semantics { heading() },
        )
        actions()
    }
}

@Composable
fun LomaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val colors = LocalLomaColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().let { if (testTag != null) it.testTag(testTag) else it },
        singleLine = singleLine,
        minLines = minLines,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = LomaShapes.Item,
        textStyle = LocalLomaTypography.current.body.copy(color = colors.textPrimary, fontSize = 16.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.primary,
            errorBorderColor = colors.alert,
            errorLabelColor = colors.alert,
            errorSupportingTextColor = colors.alert,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            errorContainerColor = colors.surface,
        ),
    )
}

/** Campo de valor em reais: aceita "R$ 1.420,00", "280,5", "300"; mostra "Valor inválido" se não der. */
@Composable
fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, testTag: String? = null, error: String? = null) {
    val invalid = value.isNotBlank() && Money.parseBrl(value) == null
    LomaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        testTag = testTag,
        keyboardType = KeyboardType.Decimal,
        error = if (invalid) stringResource(Res.string.money_invalid) else error,
    )
}

enum class PillKind { Primary, Accent, Secondary, Danger }

/** Botão em pílula (altura ≥ 52 dp), como nas telas aprovadas. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PillKind = PillKind.Primary,
    enabled: Boolean = true,
    testTag: String? = null,
    icon: ImageVector? = null,
) {
    val colors = LocalLomaColors.current
    val (bg, fg) = when (kind) {
        PillKind.Primary -> colors.primary to colors.onPrimary
        PillKind.Accent -> colors.accent to colors.onAccent
        PillKind.Secondary -> Color.Transparent to colors.textPrimary
        PillKind.Danger -> Color.Transparent to colors.alert
    }
    Row(
        modifier
            .heightIn(min = 52.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
            .background(if (enabled) bg else bg.copy(alpha = 0.4f), LomaShapes.Pill)
            .let { if (kind == PillKind.Secondary || kind == PillKind.Danger) it.border(1.5.dp, if (kind == PillKind.Danger) colors.alert else colors.border, LomaShapes.Pill) else it }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) fg else fg.copy(alpha = 0.5f), modifier = Modifier.size(18.dp).padding(end = 2.dp))
        }
        Text(
            text,
            color = if (enabled) fg else fg.copy(alpha = 0.6f),
            style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
            textAlign = TextAlign.Center,
        )
    }
}

/** Cartão branco de cantos 26 dp. */
@Composable
fun LomaCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalLomaColors.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surface, LomaShapes.Card)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(18.dp),
        content = content,
    )
}

/** Linha de menu com título, texto de apoio e seta. */
@Composable
fun MenuRow(title: String, hint: String, onClick: () -> Unit, testTag: String) {
    val colors = LocalLomaColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(testTag).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = colors.textPrimary)
            Text(hint, style = LocalLomaTypography.current.body.copy(fontSize = 14.sp), color = colors.textSecondary)
        }
        Icon(LomaIcons.Seta, contentDescription = null, tint = colors.textSecondary)
    }
}

/** Rótulo de seção (texto secundário, negrito). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
        style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
        color = LocalLomaColors.current.textSecondary,
    )
}

@Composable
internal fun Dot(color: Color, size: Int = 14) {
    Box(Modifier.size(size.dp).background(color, LomaShapes.Pill))
}
