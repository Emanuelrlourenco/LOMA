// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class AndroidMessagingLauncher(private val context: Context) : MessagingLauncher {

    override fun available(): Set<MessagingApp> = buildSet {
        if (installed(WHATSAPP)) add(MessagingApp.WHATSAPP)
        if (installed(WHATSAPP_BUSINESS)) add(MessagingApp.WHATSAPP_BUSINESS)
        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        if (sms.resolveActivity(context.packageManager) != null) add(MessagingApp.SMS)
    }

    override fun open(app: MessagingApp, phoneDigits: String, text: String): Boolean {
        val intent = when (app) {
            MessagingApp.WHATSAPP -> Intent(Intent.ACTION_VIEW, Uri.parse(WhatsAppLinks.waMe(phoneDigits, text))).setPackage(WHATSAPP)
            MessagingApp.WHATSAPP_BUSINESS -> Intent(Intent.ACTION_VIEW, Uri.parse(WhatsAppLinks.waMe(phoneDigits, text))).setPackage(WHATSAPP_BUSINESS)
            MessagingApp.SMS -> Intent(Intent.ACTION_SENDTO, Uri.parse(WhatsAppLinks.sms(phoneDigits, text))).putExtra("sms_body", text)
        }
        return start(intent)
    }

    override fun call(phoneDigits: String): Boolean = start(Intent(Intent.ACTION_DIAL, Uri.parse(WhatsAppLinks.tel(phoneDigits))))

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun installed(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val WHATSAPP = "com.whatsapp"
        const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    }
}
