// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class AppointmentRulesTest {

    private val sp = TimeZone.of("America/Sao_Paulo")
    private val coloracao = Service(id = 1, name = "Coloração", durationMinutes = 90, price = Money.ofReais(180), colorArgb = 0xFF5ED6B4)
    private val escova = Service(id = 2, name = "Escova", durationMinutes = 60, price = Money.ofReais(70), colorArgb = 0xFFB69CF2)

    private fun at(day: Int, hour: Int, minute: Int = 0, month: Int = 11, zone: TimeZone = sp): Instant =
        LocalDateTime(2026, month, day, hour, minute).toInstant(zone)

    private fun maria(start: Instant = at(20, 13)) = AppointmentRules.book(10, listOf(coloracao, escova), start, sp)

    @Test
    fun bookSomaDuracaoEValor() {
        val a = maria()
        assertEquals(at(20, 15, 30), a.end)
        assertEquals(Money.ofReais(250), a.expected)
        assertEquals(AppointmentStatus.AGENDADO, a.status)
        assertEquals(listOf("Coloração", "Escova"), a.services.map { it.name })
        assertEquals("America/Sao_Paulo", a.zoneId)
        assertEquals(10L, a.clientId)
    }

    @Test
    fun precoCopiadoNaoMudaComOServico() {
        val a = maria()
        @Suppress("UNUSED_VARIABLE")
        val novoPreco = coloracao.copy(price = Money.ofReais(200))
        assertEquals(Money.ofReais(180), a.services[0].price)
        assertEquals(1L, a.services[0].serviceId)
    }

    @Test
    fun confirmarConcluirReabrir() {
        val confirmado = AppointmentRules.confirm(maria())
        assertEquals(AppointmentStatus.CONFIRMADO, confirmado.status)
        val pago = at(20, 15, 40)
        val concluido = AppointmentRules.complete(confirmado, Money.ofReais(280), pago, "Fez hidratação")
        assertEquals(AppointmentStatus.CONCLUIDO, concluido.status)
        assertEquals(Money.ofReais(280), concluido.finalValue)
        assertEquals(pago, concluido.completedAt)
        assertEquals("Fez hidratação", concluido.note)
        val reaberto = AppointmentRules.reopen(concluido)
        assertEquals(AppointmentStatus.CONFIRMADO, reaberto.status)
        assertNull(reaberto.finalValue)
        assertNull(reaberto.completedAt)
    }

    @Test
    fun transicoesInvalidas() {
        val concluido = AppointmentRules.complete(maria(), Money.ofReais(250), at(20, 16), "")
        val desmarcado = AppointmentRules.unschedule(maria(), CancelReason.CANCELOU)
        assertFailsWith<IllegalStateException> { AppointmentRules.confirm(concluido) }
        assertFailsWith<IllegalStateException> { AppointmentRules.unschedule(desmarcado, CancelReason.CANCELOU) }
        assertFailsWith<IllegalStateException> { AppointmentRules.reopen(maria()) }
        assertFailsWith<IllegalStateException> { AppointmentRules.complete(maria(), Money(-1), at(20, 16), "") }
    }

    @Test
    fun desmarcarGuardaMotivo() {
        val d = AppointmentRules.unschedule(AppointmentRules.confirm(maria()), CancelReason.NAO_COMPARECEU)
        assertEquals(AppointmentStatus.DESMARCADO, d.status)
        assertEquals(CancelReason.NAO_COMPARECEU, d.cancelReason)
    }

    @Test
    fun semBaixa() {
        val a = maria()
        assertTrue(AppointmentRules.isWithoutSettlement(a, at(20, 15, 31)))
        assertFalse(AppointmentRules.isWithoutSettlement(a, at(20, 15, 30)))
        val concluido = AppointmentRules.complete(a, Money.ofReais(250), at(20, 16), "")
        assertFalse(AppointmentRules.isWithoutSettlement(concluido, at(21, 10)))
    }

    @Test
    fun pagamentoPadraoSemBaixaDeOntem() {
        val ontem = AppointmentRules.book(10, listOf(Service(id = 3, name = "Manicure", durationMinutes = 60, price = Money.ofReais(50), colorArgb = 0)), at(19, 23, 30), sp)
        val pago = AppointmentRules.defaultPaidAt(ontem, at(20, 10), sp)
        assertEquals(LocalDateTime(2026, 11, 19, 12, 0), pago.toLocalDateTime(sp))
        val hoje = maria()
        assertEquals(at(20, 16), AppointmentRules.defaultPaidAt(hoje, at(20, 16), sp))
    }

    @Test
    fun conflitos() {
        val a = maria().copy(id = 1)
        fun outro(id: Long, h: Int, m: Int, dur: Int) = AppointmentRules.book(
            11, listOf(Service(id = 9, name = "X", durationMinutes = dur, price = Money.ZERO, colorArgb = 0)), at(20, h, m), sp,
        ).copy(id = id)
        assertTrue(AppointmentRules.conflicts(a, listOf(outro(2, 15, 30, 60))).isEmpty())
        assertEquals(1, AppointmentRules.conflicts(a, listOf(outro(3, 15, 0, 60))).size)
        val desmarcado = AppointmentRules.unschedule(outro(4, 14, 0, 60), CancelReason.CANCELOU)
        assertTrue(AppointmentRules.conflicts(a, listOf(desmarcado)).isEmpty())
        assertTrue(AppointmentRules.conflicts(a, listOf(a)).isEmpty())
    }

    @Test
    fun repetirSemanal() {
        val copias = AppointmentRules.repeatWeekly(maria().copy(id = 7), 3)
        assertEquals(3, copias.size)
        assertEquals(listOf(27, 4, 11), copias.map { it.start.toLocalDateTime(sp).day })
        assertTrue(copias.all { it.id == 0L && it.status == AppointmentStatus.AGENDADO })
        assertTrue(copias.all { it.start.toLocalDateTime(sp).hour == 13 && it.start.toLocalDateTime(sp).minute == 0 })

        val lisboa = TimeZone.of("Europe/Lisbon")
        val l = AppointmentRules.book(10, listOf(coloracao), at(20, 13, month = 3, zone = lisboa), lisboa)
        val horas = AppointmentRules.repeatWeekly(l, 2).map { it.start.toLocalDateTime(lisboa) }
        assertEquals(listOf(LocalDateTime(2026, 3, 27, 13, 0), LocalDateTime(2026, 4, 3, 13, 0)), horas)
    }
}
