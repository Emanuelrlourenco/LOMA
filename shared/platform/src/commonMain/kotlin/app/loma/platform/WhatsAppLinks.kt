// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

/** Endereços que abrem o WhatsApp, o SMS e o discador. */
object WhatsAppLinks {
    fun waMe(phoneDigits: String, text: String): String =
        if (text.isEmpty()) "https://wa.me/$phoneDigits" else "https://wa.me/$phoneDigits?text=${encode(text)}"

    fun sms(phoneDigits: String, @Suppress("UNUSED_PARAMETER") text: String): String = "smsto:+$phoneDigits"

    fun tel(phoneDigits: String): String = "tel:+$phoneDigits"

    fun iosWhatsApp(phoneDigits: String, text: String): String = "whatsapp://send?phone=$phoneDigits&text=${encode(text)}"

    fun iosSms(phoneDigits: String, text: String): String = "sms:+$phoneDigits&body=${encode(text)}"

    /** Percent-encoding em UTF-8; só letras, dígitos e "-._~" ficam como estão. */
    fun encode(text: String): String = buildString {
        for (b in text.encodeToByteArray()) {
            val c = (b.toInt() and 0xFF)
            val ch = c.toChar()
            if (c < 0x80 && (ch.isLetterOrDigit() || ch in "-._~")) {
                append(ch)
            } else {
                append('%')
                append(HEX[c shr 4])
                append(HEX[c and 0x0F])
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
}
