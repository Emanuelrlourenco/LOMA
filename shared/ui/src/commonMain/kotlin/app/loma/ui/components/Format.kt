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

/** "13:00" a partir de hora e minuto. */
fun hhmm(hour: Int, minute: Int): String = "${twoDigits(hour)}:${twoDigits(minute)}"

/** Lê "13:00", "13h", "1300" ou "9:5" como hora e minuto; `null` se não for uma hora válida. */
fun parseTime(text: String): Pair<Int, Int>? {
    val t = text.trim().lowercase().replace('h', ':')
    val (h, m) = when {
        ':' in t -> t.substringBefore(':') to t.substringAfter(':').ifEmpty { "0" }
        t.length in 3..4 && t.all(Char::isDigit) -> t.dropLast(2) to t.takeLast(2)
        t.length in 1..2 && t.all(Char::isDigit) -> t to "0"
        else -> return null
    }
    val hour = h.toIntOrNull() ?: return null
    val minute = m.toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}
