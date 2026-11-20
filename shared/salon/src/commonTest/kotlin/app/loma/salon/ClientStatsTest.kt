// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ClientStatsTest {
    private val sp = TimeZone.of("America/Sao_Paulo")
    private fun at(day: Int): Instant = LocalDateTime(2026, 10, day, 13, 0).toInstant(sp)
    private fun done(day: Int, cents: Long) = AppointmentRules.complete(
        AppointmentRules.book(1, listOf(Service(id = 1, name = "S", durationMinutes = 60, price = Money(cents), colorArgb = 0)), at(day), sp),
        Money(cents), at(day), "",
    )

    @Test
    fun totaisDeDozeVisitas() {
        val valores = listOf(25000L, 25000, 25000, 25000, 25000, 25000, 25000, 25000, 25000, 25000, 25000, 11000)
        val s = ClientStats.of(valores.mapIndexed { i, c -> done(i + 1, c) })
        assertEquals(12, s.visits)
        assertEquals(Money.ofReais(2860), s.total)
        assertEquals(Money(23833), s.averageTicket)
        assertEquals(at(12), s.lastVisit)
    }

    @Test
    fun semConcluidos() {
        val s = ClientStats.of(emptyList())
        assertNull(s.averageTicket)
        assertNull(s.lastVisit)
        assertEquals(0, s.visits)
    }

    @Test
    fun desmarquesNaoContamRemarcar() {
        fun und(r: CancelReason) = AppointmentRules.unschedule(
            AppointmentRules.book(1, listOf(Service(id = 1, name = "S", durationMinutes = 60, price = Money(100), colorArgb = 0)), at(1), sp), r,
        )
        val s = ClientStats.of(listOf(und(CancelReason.CANCELOU), und(CancelReason.NAO_COMPARECEU), und(CancelReason.REMARCOU)))
        assertEquals(2, s.cancellations)
    }
}
