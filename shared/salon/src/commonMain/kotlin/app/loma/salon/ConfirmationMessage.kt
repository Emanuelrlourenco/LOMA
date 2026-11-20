// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Mensagem de confirmação do WhatsApp a partir do modelo editável (design §7.7). */
object ConfirmationMessage {

    const val DEFAULT_TEMPLATE: String =
        "Oi {nome}! Tudo bem? Passando para confirmar seu horário {quando} às {hora} para {servicos}. Posso confirmar?"

    /** Palavras que dependem do idioma: "hoje", "amanhã" e a conjunção da lista ("e"). */
    data class WhenLabels(val today: String, val tomorrow: String, val and: String = "e")

    fun render(
        template: String,
        clientName: String,
        start: Instant,
        zone: TimeZone,
        today: LocalDate,
        services: List<String>,
        labels: WhenLabels,
    ): String {
        val local = start.toLocalDateTime(zone)
        val date = local.date
        val quando = when (date) {
            today -> labels.today
            today.plus(DatePeriod(days = 1)) -> labels.tomorrow
            else -> "${two(date.day)}/${two(date.month.ordinal + 1)}"
        }
        val names = services.map { it.lowercase() }
        val servicos = when (names.size) {
            0 -> ""
            1 -> names[0]
            else -> names.dropLast(1).joinToString(", ") + " ${labels.and} " + names.last()
        }
        return template
            .replace("{nome}", clientName.trim().substringBefore(' '))
            .replace("{quando}", quando)
            .replace("{hora}", "${two(local.hour)}:${two(local.minute)}")
            .replace("{servicos}", servicos)
    }

    private fun two(n: Int) = n.toString().padStart(2, '0')
}
