// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Razão de contraste WCAG 2.x entre duas cores (de 1 a 21). */
fun contrastRatio(foreground: Color, background: Color): Double {
    val a = foreground.luminance().toDouble()
    val b = background.luminance().toDouble()
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}
