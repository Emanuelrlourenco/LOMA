// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.runtime.Composable
import app.loma.salon.ConfirmationMessage
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.list_and
import app.loma.ui.generated.resources.when_today
import app.loma.ui.generated.resources.when_tomorrow
import org.jetbrains.compose.resources.stringResource

/** "hoje", "amanhã" e "e" no idioma do aparelho. */
@Composable
fun whenLabels(): ConfirmationMessage.WhenLabels = ConfirmationMessage.WhenLabels(
    today = stringResource(Res.string.when_today),
    tomorrow = stringResource(Res.string.when_tomorrow),
    and = stringResource(Res.string.list_and),
)
