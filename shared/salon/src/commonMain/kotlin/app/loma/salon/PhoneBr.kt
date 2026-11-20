// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

/** Telefones: guardados só com dígitos e DDI (padrão 55, Brasil). */
object PhoneBr {

    /** Dígitos com DDI, ou `null` se não parece um telefone. */
    fun normalize(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        if (text.any { it.isLetter() }) return null
        val digits = text.filter { it.isDigit() }
        if (text.startsWith("+")) return digits.takeIf { it.length in 8..15 }
        val national = digits.trimStart('0')
        return when {
            national.length in 10..11 && national[0] != '0' -> "55$national"
            national.length in 12..13 && national.startsWith("55") -> national
            else -> null
        }
    }

    /** "(11) 98765-4321" para números do Brasil; "+<dígitos>" para os outros. */
    fun format(digits: String): String {
        if (digits.startsWith("55") && digits.length in 12..13) {
            val ddd = digits.substring(2, 4)
            val number = digits.substring(4)
            val split = number.length - 4
            return "($ddd) ${number.substring(0, split)}-${number.substring(split)}"
        }
        return "+$digits"
    }
}
