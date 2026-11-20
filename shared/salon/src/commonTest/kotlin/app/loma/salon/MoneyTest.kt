// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {

    @Test
    fun formataNoPadraoBrasileiro() {
        assertEquals("R$ 0,00", Money(0).formatBrl())
        assertEquals("R$ 70,00", Money(7000).formatBrl())
        assertEquals("R$ 1.420,00", Money(142000).formatBrl())
        assertEquals("R$ 2.960,00", Money(296000).formatBrl())
        assertEquals("R$ 1.234.567,89", Money(123456789).formatBrl())
        assertEquals("-R$ 25,00", Money(-2500).formatBrl())
        assertEquals("R$ 0,05", Money(5).formatBrl())
    }

    @Test
    fun omiteCentavosZeradosQuandoPedido() {
        assertEquals("R$ 4.380", Money(438000).formatBrl(showZeroCents = false))
        assertEquals("R$ 4.380,50", Money(438050).formatBrl(showZeroCents = false))
    }

    @Test
    fun leValoresDigitados() {
        assertEquals(Money(28000), Money.parseBrl("280,00"))
        assertEquals(Money(28000), Money.parseBrl("280"))
        assertEquals(Money(142050), Money.parseBrl("1.420,5"))
        assertEquals(Money(142000), Money.parseBrl("R$ 1.420,00"))
        assertEquals(Money(28050), Money.parseBrl(" 280,5 "))
        assertEquals(Money(-2500), Money.parseBrl("-25,00"))
    }

    @Test
    fun recusaTextoInvalido() {
        listOf("", "abc", "12,345", "1,2,3", "1.42,00").forEach { texto ->
            assertNull(Money.parseBrl(texto), "deveria recusar \"$texto\"")
        }
    }

    @Test
    fun idaEVoltaSemPerderCentavos() {
        listOf(0L, 5L, 7000L, 142000L, -2500L, 123456789L).forEach { cents ->
            val m = Money(cents)
            assertEquals(m, Money.parseBrl(m.formatBrl()))
        }
    }

    @Test
    fun somaESubtraiEmCentavos() {
        assertEquals(Money(438000), Money(118000) + Money(129000) + Money(191000))
        assertEquals(Money(296000), Money(438000) - Money(142000))
        assertEquals(Money(7000), Money.ofReais(70))
        assertEquals(Money(0), Money.ZERO)
    }
}
