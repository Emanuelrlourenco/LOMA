// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ConfirmationMessageTest {
    private val sp = TimeZone.of("America/Sao_Paulo")
    private val hoje = LocalDate(2026, 11, 20)
    private val labels = ConfirmationMessage.WhenLabels(today = "hoje", tomorrow = "amanhã")

    private fun render(day: Int, services: List<String>, template: String = ConfirmationMessage.DEFAULT_TEMPLATE) =
        ConfirmationMessage.render(template, "Maria Souza", LocalDateTime(2026, 11, day, 13, 0).toInstant(sp), sp, hoje, services, labels)

    @Test
    fun modeloPadrao() {
        assertEquals(
            "Oi Maria! Tudo bem? Passando para confirmar seu horário hoje às 13:00 para coloração e escova. Posso confirmar?",
            render(20, listOf("Coloração", "Escova")),
        )
        assertEquals(true, render(21, listOf("Escova")).contains("horário amanhã às 13:00"))
        assertEquals(true, render(25, listOf("Escova")).contains("horário 25/11 às 13:00"))
        assertEquals(true, render(20, listOf("Manicure")).endsWith("para manicure. Posso confirmar?"))
        assertEquals(true, render(20, listOf("A", "B", "C")).contains("para a, b e c."))
    }

    @Test
    fun camposDesconhecidosFicamComoEstao() {
        assertEquals("Maria {xyz}", render(20, listOf("Escova"), "{nome} {xyz}"))
    }
}
