// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.time.Instant

/** Resumo da ficha: última visita, visitas, total gasto, ticket médio e desmarques (design §7.4). */
data class ClientStats(
    val lastVisit: Instant?,
    val visits: Int,
    val total: Money,
    val averageTicket: Money?,
    val cancellations: Int,
) {
    companion object {
        fun of(appointments: List<Appointment>): ClientStats {
            val done = appointments.filter { it.status == AppointmentStatus.CONCLUIDO }
            val total = done.fold(Money.ZERO) { acc, a -> acc + (a.finalValue ?: Money.ZERO) }
            return ClientStats(
                lastVisit = done.maxOfOrNull { it.start },
                visits = done.size,
                total = total,
                averageTicket = if (done.isEmpty()) null else Money(total.cents / done.size),
                cancellations = appointments.count {
                    it.status == AppointmentStatus.DESMARCADO &&
                        (it.cancelReason == CancelReason.CANCELOU || it.cancelReason == CancelReason.NAO_COMPARECEU)
                },
            )
        }
    }
}
