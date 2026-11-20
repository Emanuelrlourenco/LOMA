// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** Curvas do CSS do protótipo. */
internal val EaseIn: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)
internal val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
internal val EaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
internal val DotEase: Easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.3f)

/**
 * Valor de uma propriedade animada como no CSS (`animation: ... <dur> <easing> <delay> both`):
 * antes do início vale o primeiro quadro, depois do fim vale o último, e a curva vale por trecho.
 */
internal fun keyframes(elapsedMs: Long, delayMs: Int, durationMs: Int, easing: Easing, vararg stops: Pair<Float, Float>): Float {
    val p = ((elapsedMs - delayMs).toFloat() / durationMs).coerceIn(0f, 1f)
    for (i in 0 until stops.size - 1) {
        val (a, va) = stops[i]
        val (b, vb) = stops[i + 1]
        if (p <= b || i == stops.size - 2) {
            val local = if (b > a) ((p - a) / (b - a)).coerceIn(0f, 1f) else 1f
            return va + (vb - va) * easing.transform(local)
        }
    }
    return stops.last().second
}
