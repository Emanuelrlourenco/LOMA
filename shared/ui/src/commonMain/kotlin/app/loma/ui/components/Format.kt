// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.components

import app.loma.salon.Money

/** Duração como nas telas aprovadas: "1h30", "1h", "45 min", "2h05". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "${h}h"
        else -> "${h}h${m.toString().padStart(2, '0')}"
    }
}

/** Valor como texto editável: "180" ou "180,50". */
fun Money.toInput(): String {
    val reais = cents / 100
    val c = kotlin.math.abs(cents % 100)
    return if (c == 0L) reais.toString() else "$reais,${c.toString().padStart(2, '0')}"
}

fun twoDigits(n: Int): String = n.toString().padStart(2, '0')
