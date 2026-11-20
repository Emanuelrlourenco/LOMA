// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class DaySummaryTest {
    private val sp = TimeZone.of("America/Sao_Paulo")
    private val hoje = LocalDate(2026, 11, 20)
    private fun at(day: Int, h: Int, m: Int = 0): Instant = LocalDateTime(2026, 11, day, h, m).toInstant(sp)
    private fun appt(id: Long, start: Instant, reais: Long) = AppointmentRules.book(
        id, listOf(Service(id = 1, name = "S", durationMinutes = 60, price = Money.ofReais(reais), colorArgb = 0)), start, sp,
    ).copy(id = id)

    @Test
    fun previstoERecebido() {
        val lista = listOf(
            appt(1, at(20, 13), 250),
            AppointmentRules.confirm(appt(2, at(20, 16), 80)),
            AppointmentRules.complete(appt(3, at(20, 9), 70), Money.ofReais(70), at(20, 10), ""),
            AppointmentRules.unschedule(appt(4, at(20, 11), 100), CancelReason.CANCELOU),
            AppointmentRules.complete(appt(5, at(19, 15), 60), Money.ofReais(60), at(20, 9), ""),
        )
        val s = DaySummary.of(lista, hoje, sp)
        assertEquals(Money.ofReais(330), s.expected)
        assertEquals(Money.ofReais(130), s.received)
    }

    @Test
    fun meiaNoite() {
        val a = AppointmentRules.complete(appt(1, at(19, 23, 30), 90), Money.ofReais(90), at(20, 0, 10), "")
        assertEquals(Money.ofReais(90), DaySummary.of(listOf(a), hoje, sp).received)
        assertEquals(Money.ZERO, DaySummary.of(listOf(a), LocalDate(2026, 11, 19), sp).received)
    }
}
