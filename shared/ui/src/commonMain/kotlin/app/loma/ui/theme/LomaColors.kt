// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Tokens de cor das telas aprovadas (design §7.12). */
object LomaPalette {
    val Background = Color(0xFFF3F2FA)
    val Surface = Color(0xFFFFFFFF)
    val Container = Color(0xFFE7E4F8)
    val Navy = Color(0xFF17165C)
    val NavyLight = Color(0xFF2A2985)
    val Mint = Color(0xFF5ED6B4)
    val OnMint = Color(0xFF0B3A2E)
    val MintSoft = Color(0xFFE3F8F1)
    val Lilac = Color(0xFFB69CF2)
    val Coral = Color(0xFFFF7A66)
    val AlertSoft = Color(0xFFFFE2DC)
    val OnAlertSoft = Color(0xFF7A1A0B)
    val TextSecondary = Color(0xFF5B5A7A)
    val Border = Color(0xFFC9C6E6)
}

@Immutable
data class LomaColors(
    val background: Color,
    val surface: Color,
    val container: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val selection: Color,
    val alert: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val navIndicator: Color,
    val navIndicatorIcon: Color,
)

val LightLomaColors = LomaColors(
    background = LomaPalette.Background,
    surface = LomaPalette.Surface,
    container = LomaPalette.Container,
    primary = LomaPalette.Navy,
    onPrimary = Color.White,
    accent = LomaPalette.Mint,
    onAccent = LomaPalette.OnMint,
    selection = LomaPalette.Lilac,
    alert = LomaPalette.Coral,
    textPrimary = LomaPalette.Navy,
    textSecondary = LomaPalette.TextSecondary,
    border = LomaPalette.Border,
    navIndicator = LomaPalette.Navy,
    navIndicatorIcon = LomaPalette.Mint,
)

val DarkLomaColors = LomaColors(
    background = Color(0xFF0F0E33),
    surface = Color(0xFF1B1A52),
    container = Color(0xFF2A2985),
    primary = LomaPalette.Mint,
    onPrimary = LomaPalette.OnMint,
    accent = LomaPalette.Mint,
    onAccent = LomaPalette.OnMint,
    selection = LomaPalette.Lilac,
    alert = LomaPalette.Coral,
    textPrimary = Color(0xFFF3F2FA),
    textSecondary = Color(0xFFC9C6F0),
    border = Color(0xFF3A3994),
    navIndicator = LomaPalette.Mint,
    navIndicatorIcon = LomaPalette.OnMint,
)

val LocalLomaColors = staticCompositionLocalOf { LightLomaColors }
