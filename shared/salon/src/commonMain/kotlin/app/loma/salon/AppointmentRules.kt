// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Regras de status dos atendimentos (design §7.4 e regras 1–4 do §7.5). */
object AppointmentRules {

    fun book(clientId: Long, services: List<Service>, start: Instant, zone: TimeZone): Appointment {
        val booked = services.map { BookedService(it.id, it.name, it.durationMinutes, it.price) }
        return Appointment(
            clientId = clientId,
            start = start,
            end = start + booked.sumOf { it.durationMinutes }.minutes,
            zoneId = zone.id,
            status = AppointmentStatus.AGENDADO,
            expected = booked.fold(Money.ZERO) { acc, s -> acc + s.price },
            services = booked,
        )
    }

    fun confirm(a: Appointment): Appointment {
        check(a.status == AppointmentStatus.AGENDADO) { "só um atendimento agendado pode ser confirmado (está ${a.status})" }
        return a.copy(status = AppointmentStatus.CONFIRMADO)
    }

    fun complete(a: Appointment, finalValue: Money, paidAt: Instant, note: String): Appointment {
        check(a.isOpen) { "só um atendimento aberto pode ser concluído (está ${a.status})" }
        check(finalValue >= Money.ZERO) { "o valor final não pode ser negativo" }
        return a.copy(status = AppointmentStatus.CONCLUIDO, finalValue = finalValue, completedAt = paidAt, note = note, cancelReason = null)
    }

    fun unschedule(a: Appointment, reason: CancelReason): Appointment {
        check(a.isOpen) { "só um atendimento aberto pode ser desmarcado (está ${a.status})" }
        return a.copy(status = AppointmentStatus.DESMARCADO, cancelReason = reason)
    }

    fun reopen(a: Appointment): Appointment {
        check(a.status == AppointmentStatus.CONCLUIDO) { "só um atendimento concluído pode ser reaberto (está ${a.status})" }
        return a.copy(status = AppointmentStatus.CONFIRMADO, finalValue = null, completedAt = null)
    }

    /** Aberto e já terminado: "sem baixa". */
    fun isWithoutSettlement(a: Appointment, now: Instant): Boolean = a.isOpen && a.end < now

    /** Data do pagamento sugerida: agora; para um "sem baixa" de outro dia, meio-dia do dia do atendimento. */
    fun defaultPaidAt(a: Appointment, now: Instant, zone: TimeZone): Instant {
        val day = a.start.toLocalDateTime(zone).date
        return if (isWithoutSettlement(a, now) && day != now.toLocalDateTime(zone).date) {
            day.atTime(12, 0).toInstant(zone)
        } else {
            now
        }
    }

    /** Atendimentos (não desmarcados, com outro id) que se sobrepõem ao candidato; encostar não conta. */
    fun conflicts(candidate: Appointment, others: List<Appointment>): List<Appointment> = others.filter {
        it.status != AppointmentStatus.DESMARCADO &&
            (candidate.id == 0L || it.id != candidate.id) &&
            it.start < candidate.end && candidate.start < it.end
    }

    /** [times] cópias semanais, no mesmo horário local do fuso do atendimento. */
    fun repeatWeekly(a: Appointment, times: Int): List<Appointment> {
        val zone = TimeZone.of(a.zoneId)
        val local = a.start.toLocalDateTime(zone)
        val length = a.end - a.start
        return (1..times).map { week ->
            val start = LocalDateTime(local.date.plus(DatePeriod(days = 7 * week)), local.time).toInstant(zone)
            a.copy(
                id = 0, start = start, end = start + length,
                status = AppointmentStatus.AGENDADO, cancelReason = null, finalValue = null, completedAt = null,
            )
        }
    }
}
