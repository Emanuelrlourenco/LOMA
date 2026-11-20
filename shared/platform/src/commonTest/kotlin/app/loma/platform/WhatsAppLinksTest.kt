// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class WhatsAppLinksTest {
    @Test
    fun linkDoWhatsAppComTexto() {
        assertEquals(
            "https://wa.me/5511987654321?text=Oi%20Maria%21%20%C3%80s%2013%3A00%20%26%20tal",
            WhatsAppLinks.waMe("5511987654321", "Oi Maria! Às 13:00 & tal"),
        )
    }

    @Test
    fun linkDoWhatsAppSemTexto() {
        assertEquals("https://wa.me/5511987654321", WhatsAppLinks.waMe("5511987654321", ""))
    }

    @Test
    fun linksDeSmsETelefone() {
        assertEquals("smsto:+5511987654321", WhatsAppLinks.sms("5511987654321", "Oi"))
        assertEquals("tel:+5511987654321", WhatsAppLinks.tel("5511987654321"))
    }

    @Test
    fun linksDoIos() {
        assertEquals("whatsapp://send?phone=5511987654321&text=Oi%20Maria", WhatsAppLinks.iosWhatsApp("5511987654321", "Oi Maria"))
        assertEquals("sms:+5511987654321&body=Oi%20Maria", WhatsAppLinks.iosSms("5511987654321", "Oi Maria"))
    }
}
