// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.placeholder_soon
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import org.jetbrains.compose.resources.stringResource

/** Tela provisória das abas que chegam nas próximas fases. */
@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    val colors = LocalLomaColors.current
    val type = LocalLomaTypography.current
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Text(
            title,
            style = type.screenTitle,
            color = colors.textPrimary,
            modifier = Modifier.testTag("screen_title").semantics { heading() },
        )
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.placeholder_soon),
                style = type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
