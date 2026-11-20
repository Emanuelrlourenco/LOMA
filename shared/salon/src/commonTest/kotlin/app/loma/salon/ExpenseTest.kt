// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class ExpenseTest {
    private val luz = RecurringExpense(id = 7, category = ExpenseCategory.ENERGIA, name = "Energia", amount = Money.ofReais(180), dueDay = 31, startYear = 2026, startMonth = 1)

    @Test
    fun dia31EmFevereiro() {
        assertEquals(LocalDate(2026, 2, 28), luz.dueDate(2026, 2))
        assertEquals(LocalDate(2028, 2, 29), luz.dueDate(2028, 2))
    }

    @Test
    fun dia31EmAbril() {
        assertEquals(LocalDate(2026, 4, 30), luz.dueDate(2026, 4))
        assertEquals(LocalDate(2026, 5, 31), luz.dueDate(2026, 5))
    }

    @Test
    fun antesDoInicioNaoLanca() {
        assertNull(luz.copy(startYear = 2026, startMonth = 11).billFor(2026, 10))
        assertNull(luz.billFor(2025, 12))
    }

    @Test
    fun inativoNaoLanca() {
        assertNull(luz.copy(active = false).billFor(2026, 11))
    }

    @Test
    fun contaLancadaPendente() {
        assertEquals(
            Expense(category = ExpenseCategory.ENERGIA, name = "Energia", amount = Money.ofReais(180), date = LocalDate(2026, 11, 30), paidOn = null, recurringId = 7),
            luz.billFor(2026, 11),
        )
        assertEquals(ExpenseKind.FIXA, ExpenseCategory.ENERGIA.kind)
        assertEquals(ExpenseKind.MATERIAL, ExpenseCategory.PRODUTOS.kind)
    }
}
