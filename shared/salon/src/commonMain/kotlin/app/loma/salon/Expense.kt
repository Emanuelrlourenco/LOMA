// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlinx.datetime.LocalDate

enum class ExpenseKind { FIXA, MATERIAL }

/** Categorias pré-cadastradas (design §7.5). */
enum class ExpenseCategory(val kind: ExpenseKind) {
    ALUGUEL(ExpenseKind.FIXA), ENERGIA(ExpenseKind.FIXA), AGUA(ExpenseKind.FIXA), INTERNET(ExpenseKind.FIXA), OUTRA_FIXA(ExpenseKind.FIXA),
    PRODUTOS(ExpenseKind.MATERIAL), DESCARTAVEIS(ExpenseKind.MATERIAL), EQUIPAMENTOS(ExpenseKind.MATERIAL), OUTRO_MATERIAL(ExpenseKind.MATERIAL),
}

/** Gasto: conta fixa ([date] = vencimento) ou material ([date] = data da compra, já pago). */
data class Expense(
    val id: Long = 0,
    val category: ExpenseCategory,
    val name: String,
    val amount: Money,
    val date: LocalDate,
    val paidOn: LocalDate?,
    val recurringId: Long? = null,
    val note: String = "",
) {
    val kind: ExpenseKind get() = category.kind
    val paid: Boolean get() = paidOn != null
}

/** Modelo de conta fixa mensal: lança uma conta pendente por mês a partir de [startYear]/[startMonth]. */
data class RecurringExpense(
    val id: Long = 0,
    val category: ExpenseCategory,
    val name: String,
    val amount: Money,
    val dueDay: Int,
    val startYear: Int,
    val startMonth: Int,
    val active: Boolean = true,
) {
    /** Vencimento no mês; se o mês não tem [dueDay], o último dia. */
    fun dueDate(year: Int, month: Int): LocalDate = LocalDate(year, month, dueDay.coerceIn(1, lastDay(year, month)))

    /** Conta do mês, ou `null` antes do início ou com o modelo desligado. */
    fun billFor(year: Int, month: Int): Expense? {
        if (!active || year * 12 + month < startYear * 12 + startMonth) return null
        return Expense(category = category, name = name, amount = amount, date = dueDate(year, month), paidOn = null, recurringId = id)
    }
}

internal fun lastDay(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return next.toEpochDays().minus(first.toEpochDays()).toInt()
}

