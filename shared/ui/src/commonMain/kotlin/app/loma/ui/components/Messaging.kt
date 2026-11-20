// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.components

import app.loma.platform.MessagingApp

/** App de mensagem a usar: o escolhido antes (se ainda existe), senão WhatsApp, Business e por último SMS. */
fun preferredMessagingApp(available: Set<MessagingApp>, stored: String?): MessagingApp? {
    val chosen = stored?.let { runCatching { MessagingApp.valueOf(it) }.getOrNull() }
    if (chosen != null && chosen in available) return chosen
    return listOf(MessagingApp.WHATSAPP, MessagingApp.WHATSAPP_BUSINESS, MessagingApp.SMS).firstOrNull { it in available }
}
