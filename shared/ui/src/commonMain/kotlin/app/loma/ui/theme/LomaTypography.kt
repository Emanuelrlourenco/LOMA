// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.outfit_bold
import app.loma.ui.generated.resources.outfit_extrabold
import app.loma.ui.generated.resources.outfit_medium
import app.loma.ui.generated.resources.outfit_regular
import app.loma.ui.generated.resources.outfit_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class LomaTypography(
    /** Título de tela: 30 sp, ExtraBold. */
    val screenTitle: TextStyle,
    /** Nome da cliente nos cartões: 26 sp, ExtraBold. */
    val cardName: TextStyle,
    /** Texto corrido: 15 sp, Normal. */
    val body: TextStyle,
    /** Rótulo da barra inferior: 12 sp, SemiBold. */
    val navLabel: TextStyle,
)

@Composable
fun outfitFamily(): FontFamily = FontFamily(
    Font(Res.font.outfit_regular, FontWeight.Normal),
    Font(Res.font.outfit_medium, FontWeight.Medium),
    Font(Res.font.outfit_semibold, FontWeight.SemiBold),
    Font(Res.font.outfit_bold, FontWeight.Bold),
    Font(Res.font.outfit_extrabold, FontWeight.ExtraBold),
)

fun lomaTypography(family: FontFamily): LomaTypography = LomaTypography(
    screenTitle = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    cardName = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
    body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    navLabel = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
)

internal fun materialTypography(family: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

val LocalLomaTypography = staticCompositionLocalOf { lomaTypography(FontFamily.Default) }
