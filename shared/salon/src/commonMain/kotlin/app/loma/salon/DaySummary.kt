// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** "previsto R$ X · recebido R$ Y" de um dia (design §7.5, restrito ao dia). */
data class DaySummary(val expected: Money, val received: Money) {
    companion object {
        fun of(appointments: List<Appointment>, day: LocalDate, zone: TimeZone): DaySummary {
            val expected = appointments
                .filter { it.isOpen && it.start.toLocalDateTime(zone).date == day }
                .fold(Money.ZERO) { acc, a -> acc + a.expected }
            val received = appointments
                .filter { it.status == AppointmentStatus.CONCLUIDO && it.completedAt?.toLocalDateTime(zone)?.date == day }
                .fold(Money.ZERO) { acc, a -> acc + (a.finalValue ?: Money.ZERO) }
            return DaySummary(expected, received)
        }
    }
}
