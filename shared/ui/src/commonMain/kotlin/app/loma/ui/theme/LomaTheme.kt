// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun LomaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkLomaColors else LightLomaColors
    val family = outfitFamily()
    val typography = remember(family) { lomaTypography(family) }
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.selection, onSecondary = LomaPalette.Navy,
            background = colors.background, onBackground = colors.textPrimary,
            surface = colors.surface, onSurface = colors.textPrimary,
            surfaceVariant = colors.container, onSurfaceVariant = colors.textSecondary,
            outline = colors.border, error = colors.alert,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.accent, onSecondary = colors.onAccent,
            tertiary = colors.selection, onTertiary = LomaPalette.Navy,
            background = colors.background, onBackground = colors.textPrimary,
            surface = colors.surface, onSurface = colors.textPrimary,
            surfaceVariant = colors.container, onSurfaceVariant = colors.textSecondary,
            outline = colors.border, error = colors.alert,
        )
    }
    CompositionLocalProvider(LocalLomaColors provides colors, LocalLomaTypography provides typography) {
        MaterialTheme(
            colorScheme = scheme,
            typography = remember(family) { materialTypography(family) },
            shapes = LomaShapes.material,
            content = content,
        )
    }
}
