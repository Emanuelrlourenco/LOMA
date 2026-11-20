// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.appointments

import androidx.compose.runtime.Composable
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.months_short
import app.loma.ui.generated.resources.weekdays_short
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource

/** "Sex, 20 nov" (pt) / "Fri, 20 Nov" (en). */
@Composable
fun dayLabel(date: LocalDate): String {
    val days = stringArrayResource(Res.array.weekdays_short)
    val months = stringArrayResource(Res.array.months_short)
    val wd = days.getOrElse(date.dayOfWeek.ordinal.let { (it + 1) % 7 }) { "" }.replaceFirstChar { it.uppercase() }
    return "$wd, ${date.day} ${months.getOrElse(date.month.ordinal) { "" }}"
}
