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

class FinanceTest {
    private val sp = TimeZone.of("America/Sao_Paulo")
    private val hoje = LocalDate(2026, 11, 20) // sexta
    private val agora = at(20, 10)
    private val novembro = Period.month(hoje)
    private fun at(day: Int, h: Int, m: Int = 0, month: Int = 11): Instant = LocalDateTime(2026, month, day, h, m).toInstant(sp)
    private fun appt(id: Long, start: Instant, reais: Long) = AppointmentRules.book(
        id, listOf(Service(id = 1, name = "S", durationMinutes = 60, price = Money.ofReais(reais), colorArgb = 0)), start, sp,
    ).copy(id = id)
    private fun concluido(id: Long, start: Instant, final: Long, pagoEm: Instant = start) =
        AppointmentRules.complete(appt(id, start, final), Money.ofReais(final), pagoEm, "")
    private fun d(day: Int, month: Int = 11) = LocalDate(2026, month, day)
    private fun fixa(reais: Long, venc: LocalDate, pago: LocalDate?) = Expense(category = ExpenseCategory.ENERGIA, name = "Energia", amount = Money.ofReais(reais), date = venc, paidOn = pago)
    private fun material(reais: Long, data: LocalDate) = Expense(category = ExpenseCategory.PRODUTOS, name = "Tintas", amount = Money.ofReais(reais), date = data, paidOn = data)

    @Test
    fun agendarEntraEmAReceber() {
        assertEquals(Money.ofReais(250), Finance.receivable(listOf(appt(1, at(25, 13), 250)), null, agora, sp))
    }

    @Test
    fun concluirSaiDaPrevisaoESomaValorFinal() {
        val a = appt(1, at(20, 13), 250)
        val feito = AppointmentRules.complete(a, Money.ofReais(230), at(20, 15), "")
        assertEquals(Money.ZERO, Finance.receivable(listOf(feito), null, agora, sp))
        assertEquals(Money.ofReais(230), Finance.realized(listOf(feito), Period.day(hoje), sp))
    }

    @Test
    fun desmarcarTiraDaPrevisaoSemTocarRealizados() {
        val lista = listOf(AppointmentRules.unschedule(appt(1, at(25, 13), 250), CancelReason.CANCELOU), concluido(2, at(19, 10), 70))
        assertEquals(Money.ZERO, Finance.receivable(lista, null, agora, sp))
        assertEquals(Money.ofReais(70), Finance.realized(lista, novembro, sp))
    }

    @Test
    fun conclusaoPertoDaMeiaNoiteContaNoDiaDaConclusao() {
        val lista = listOf(concluido(1, at(19, 20), 100, pagoEm = at(19, 23, 59)), concluido(2, at(19, 21), 50, pagoEm = at(20, 0, 1)))
        assertEquals(Money.ofReais(100), Finance.realized(lista, Period.day(d(19)), sp))
        assertEquals(Money.ofReais(50), Finance.realized(lista, Period.day(d(20)), sp))
    }

    @Test
    fun pagoHojeDeAtendimentoDeOntem() {
        val lista = listOf(concluido(1, at(19, 15), 60, pagoEm = at(20, 9)))
        assertEquals(Money.ofReais(60), Finance.realized(lista, Period.day(hoje), sp))
        assertEquals(listOf(1L), Finance.realizedList(lista, Period.day(hoje), sp).map { it.id })
    }

    @Test
    fun semBaixaContinuaEmAReceber() {
        val semBaixa = appt(1, at(18, 13), 120) // terminou e não foi concluído
        assertEquals(Money.ofReais(120), Finance.receivable(listOf(semBaixa), Period.day(hoje), agora, sp))
    }

    @Test
    fun receberRestanteDaSemana() {
        val lista = listOf(
            appt(1, at(20, 15), 100), // hoje
            appt(2, at(21, 9), 50), // sábado
            appt(3, at(23, 9), 999), // segunda seguinte
        )
        val restante = Period(hoje, Period.week(hoje).endExclusive)
        assertEquals(Money.ofReais(150), Finance.receivable(lista, restante, agora, sp))
    }

    @Test
    fun semanaComecaNoDomingo() {
        assertEquals(Period(d(15), d(22)), Period.week(hoje))
        assertEquals(Period(d(15), d(22)), Period.week(d(15)))
        assertEquals(Period(d(1), d(1, month = 12)), Period.month(hoje))
        assertEquals(Period(d(10), d(13)), Period.custom(d(10), d(12)))
    }

    @Test
    fun lucroDescontaSoPagos() {
        val atendimentos = listOf(concluido(1, at(5, 10), 4_000), concluido(2, at(19, 10), 380))
        val gastos = listOf(fixa(900, d(10), d(10)), fixa(180, d(15), d(15)), material(240, d(12)), material(100, d(5)), fixa(180, d(21), null))
        assertEquals(Money.ofReais(2_960), Finance.netProfit(atendimentos, gastos, novembro, sp))
        assertEquals(ExpenseTotals(Money.ofReais(1_600), Money.ofReais(1_420), Money.ofReais(180)), Finance.expenseTotals(gastos, novembro))
    }

    @Test
    fun contaPagaEmOutroMes() {
        val conta = fixa(120, d(30, month = 10), d(3))
        val outubro = Period.month(d(1, month = 10))
        assertEquals(Money.ofReais(120), Finance.expenseTotals(listOf(conta), outubro).total)
        assertEquals(Money.ofReais(120), Finance.expenseTotals(listOf(conta), outubro).paid)
        assertEquals(Money.ZERO, Finance.paidIn(listOf(conta), outubro))
        assertEquals(Money.ofReais(120), Finance.paidIn(listOf(conta), novembro))
    }

    @Test
    fun centavosSemPerda() {
        val lista = (1L..3L).map { AppointmentRules.complete(appt(it, at(20, 9 + it.toInt()), 0), Money(3_333), at(20, 12), "") }
        assertEquals(Money(9_999), Finance.realized(lista, Period.day(hoje), sp))
    }

    @Test
    fun colunasDoMes() {
        val lista = listOf(concluido(1, at(3, 10), 100), concluido(2, at(20, 10), 70))
        val cols = Finance.columns(lista, listOf(material(40, d(9))), novembro, sp)
        assertEquals(listOf("1–7", "8–14", "15–21", "22–28", "29–30"), cols.map { it.label })
        assertEquals(Money.ofReais(100), cols[0].income)
        assertEquals(Money.ofReais(40), cols[1].outcome)
        assertEquals(Money.ofReais(70), cols[2].income)
    }

    @Test
    fun colunasDeUmDia() {
        val cols = Finance.columns(listOf(concluido(1, at(20, 10), 70)), emptyList(), Period.day(hoje), sp)
        assertEquals(listOf(Column("20", Money.ofReais(70), Money.ZERO)), cols)
    }

    @Test
    fun colunasCruzandoMeses() {
        val cols = Finance.columns(emptyList(), emptyList(), Period.custom(d(28), d(10, month = 12)), sp)
        assertEquals(listOf("28/11–4/12", "5/12–10/12"), cols.map { it.label })
    }
}
