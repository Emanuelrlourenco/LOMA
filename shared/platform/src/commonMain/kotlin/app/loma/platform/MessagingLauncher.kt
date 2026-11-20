// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

enum class MessagingApp { WHATSAPP, WHATSAPP_BUSINESS, SMS }

/** Abre outro app com a mensagem pronta. O LOMA não envia nada: quem envia é a profissional. */
interface MessagingLauncher {
    fun available(): Set<MessagingApp>
    fun open(app: MessagingApp, phoneDigits: String, text: String): Boolean
    fun call(phoneDigits: String): Boolean
}
