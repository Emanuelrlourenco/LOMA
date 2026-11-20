// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosMessagingLauncher : MessagingLauncher {

    override fun available(): Set<MessagingApp> = buildSet {
        if (canOpen("whatsapp://send")) add(MessagingApp.WHATSAPP)
        add(MessagingApp.SMS)
    }

    override fun open(app: MessagingApp, phoneDigits: String, text: String): Boolean = openUrl(
        when (app) {
            MessagingApp.WHATSAPP, MessagingApp.WHATSAPP_BUSINESS -> WhatsAppLinks.iosWhatsApp(phoneDigits, text)
            MessagingApp.SMS -> WhatsAppLinks.iosSms(phoneDigits, text)
        },
    )

    override fun call(phoneDigits: String): Boolean = openUrl(WhatsAppLinks.tel(phoneDigits))

    private fun canOpen(url: String): Boolean =
        NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.canOpenURL(it) } ?: false

    private fun openUrl(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }
}
