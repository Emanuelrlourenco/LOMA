// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.jvm.JvmInline

/** Valor em reais guardado em centavos. Nunca usa ponto flutuante. */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO: Money = Money(0)

        fun ofReais(reais: Long): Money = Money(reais * 100)

        private val NUMBER = Regex("""^(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{1,2}))?$""")

        /**
         * Lê um valor digitado no padrão brasileiro: "280", "280,5", "1.420,00", "R$ 1.420,00", "-25,00".
         * Pontos só valem como separador de milhar (grupos de 3). Devolve null se o texto não for um valor.
         */
        fun parseBrl(text: String): Money? {
            var t = text.trim()
            var negative = false
            if (t.startsWith("-")) {
                negative = true
                t = t.substring(1).trim()
            }
            if (t.startsWith("R$")) t = t.substring(2).trim()
            val match = NUMBER.matchEntire(t) ?: return null
            val reais = match.groupValues[1].replace(".", "").toLongOrNull() ?: return null
            val fraction = match.groupValues[2]
            val centsPart = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 10
                else -> fraction.toLong()
            }
            val total = reais * 100 + centsPart
            return Money(if (negative) -total else total)
        }
    }
}

/** "R$ 1.420,00"; negativos como "-R$ 25,00". Com [showZeroCents] = false, "R$ 4.380" quando os centavos são zero. */
fun Money.formatBrl(showZeroCents: Boolean = true): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val reais = abs / 100
    val centsPart = abs % 100
    val grouped = reais.toString().reversed().chunked(3).joinToString(".").reversed()
    val body = if (!showZeroCents && centsPart == 0L) grouped else "$grouped,${centsPart.toString().padStart(2, '0')}"
    return (if (negative) "-" else "") + "R$ " + body
}
