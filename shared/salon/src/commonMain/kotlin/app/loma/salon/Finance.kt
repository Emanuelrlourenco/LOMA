// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Intervalo de dias de calendário [start, endExclusive). */
data class Period(val start: LocalDate, val endExclusive: LocalDate) {
    operator fun contains(d: LocalDate): Boolean = d >= start && d < endExclusive
    val days: Int get() = (endExclusive.toEpochDays() - start.toEpochDays()).toInt()

    companion object {
        fun day(d: LocalDate) = Period(d, d.plus(DatePeriod(days = 1)))

        /** Semana de domingo a sábado (decisão de 20/11/2026). */
        fun week(d: LocalDate): Period {
            val back = d.dayOfWeek.ordinal.let { (it - DayOfWeek.SUNDAY.ordinal + 7) % 7 }
            val start = d.minus(DatePeriod(days = back))
            return Period(start, start.plus(DatePeriod(days = 7)))
        }

        fun month(d: LocalDate): Period {
            val start = LocalDate(d.year, d.month.number, 1)
            return Period(start, start.plus(DatePeriod(months = 1)))
        }

        fun custom(from: LocalDate, toInclusive: LocalDate): Period {
            val (a, b) = if (from <= toInclusive) from to toInclusive else toInclusive to from
            return Period(a, b.plus(DatePeriod(days = 1)))
        }
    }
}

data class ExpenseTotals(val total: Money, val paid: Money, val due: Money)

/** Coluna do gráfico Entradas × Saídas. */
data class Column(val label: String, val income: Money, val outcome: Money)

/**
 * Indicadores do §7.5, sempre calculados sobre as listas (nada acumulado): as regras 1–3
 * valem por construção.
 */
object Finance {
    private fun List<Money>.sum(): Money = fold(Money.ZERO) { acc, m -> acc + m }

    private fun Appointment.paidDay(zone: TimeZone): LocalDate? =
        if (status == AppointmentStatus.CONCLUIDO) completedAt?.toLocalDateTime(zone)?.date else null

    /** Concluídos com a conclusão no período, mais recente primeiro. */
    fun realizedList(a: List<Appointment>, p: Period, zone: TimeZone): List<Appointment> =
        a.filter { it.paidDay(zone)?.let { d -> d in p } == true }.sortedByDescending { it.completedAt }

    /** Soma do valor final dos concluídos com a conclusão no período. */
    fun realized(a: List<Appointment>, p: Period, zone: TimeZone): Money =
        realizedList(a, p, zone).map { it.finalValue ?: Money.ZERO }.sum()

    /** Valor esperado dos abertos com início no período ([p] null = todos), mais os sem baixa. */
    fun receivable(a: List<Appointment>, p: Period?, now: Instant, zone: TimeZone): Money =
        a.filter { it.isOpen && (p == null || it.start.toLocalDateTime(zone).date in p || it.end < now) }
            .map { it.expected }.sum()

    /** Gastos com data (vencimento ou compra) no período: total, pagos e a vencer. */
    fun expenseTotals(e: List<Expense>, p: Period): ExpenseTotals {
        val inPeriod = e.filter { it.date in p }
        val paid = inPeriod.filter { it.paid }.map { it.amount }.sum()
        val due = inPeriod.filter { !it.paid }.map { it.amount }.sum()
        return ExpenseTotals(paid + due, paid, due)
    }

    /** Gastos pagos no período (pela data do pagamento). */
    fun paidIn(e: List<Expense>, p: Period): Money = e.filter { it.paidOn?.let { d -> d in p } == true }.map { it.amount }.sum()

    /** Ganhos realizados − gastos pagos no período. */
    fun netProfit(a: List<Appointment>, e: List<Expense>, p: Period, zone: TimeZone): Money = realized(a, p, zone) - paidIn(e, p)

    /** Até 7 dias: uma coluna por dia; senão blocos de 7 dias a partir do início. */
    fun columns(a: List<Appointment>, e: List<Expense>, p: Period, zone: TimeZone): List<Column> {
        val blocks = if (p.days <= 7) {
            (0 until p.days).map { p.start.plus(DatePeriod(days = it)).let { d -> Period.day(d) } }
        } else {
            generateSequence(p.start) { it.plus(DatePeriod(days = 7)) }.takeWhile { it < p.endExclusive }
                .map { s -> Period(s, minOf(s.plus(DatePeriod(days = 7)), p.endExclusive)) }.toList()
        }
        return blocks.map { b -> Column(label(b, single = p.days <= 7), realized(a, b, zone), paidIn(e, b)) }
    }

    private fun label(b: Period, single: Boolean): String {
        val last = b.endExclusive.minus(DatePeriod(days = 1))
        if (single) return b.start.day.toString()
        return if (b.start.month == last.month) {
            "${b.start.day}–${last.day}"
        } else {
            "${b.start.day}/${b.start.month.number}–${last.day}/${last.month.number}"
        }
    }
}
