// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.android

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.Element

/** Garantias do manifest mesclado (debug): sem internet, sem permissões perigosas, backup só local. */
class ManifestTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private val manifest: Element by lazy {
        val file = File("build/intermediates/merged_manifest/debug").walkTopDown()
            .firstOrNull { it.name == "AndroidManifest.xml" }
            ?: error("manifest mesclado não encontrado; rode processDebugMainManifest")
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file).documentElement
    }

    private fun permissions(): List<String> {
        val nodes = manifest.getElementsByTagName("uses-permission")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(androidNs, "name") }
    }

    private val application: Element
        get() = manifest.getElementsByTagName("application").item(0) as Element

    @Test
    fun naoPedeInternet() {
        assertFalse("android.permission.INTERNET" in permissions(), "o LOMA nunca acessa a internet")
    }

    @Test
    fun naoDeclaraPermissoesPerigosas() {
        val perigosas = setOf(
            "READ_CALENDAR", "WRITE_CALENDAR", "CAMERA", "READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS",
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION", "RECORD_AUDIO",
            "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "CALL_PHONE", "READ_CALL_LOG", "WRITE_CALL_LOG",
            "SEND_SMS", "RECEIVE_SMS", "READ_SMS", "BODY_SENSORS", "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO",
            "POST_NOTIFICATIONS", "NEARBY_WIFI_DEVICES", "BLUETOOTH_CONNECT", "BLUETOOTH_SCAN",
        ).map { "android.permission.$it" }
        assertEquals(emptyList(), permissions().filter { it in perigosas })
    }

    @Test
    fun identificadorDoApp() {
        assertEquals("app.loma.calendar", manifest.getAttribute("package"))
    }

    @Test
    fun backupSoLocal() {
        assertEquals("true", application.getAttributeNS(androidNs, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(androidNs, "dataExtractionRules"))
        assertEquals("@xml/backup_rules", application.getAttributeNS(androidNs, "fullBackupContent"))
    }

    @Test
    fun abreNaMainActivity() {
        val activities = manifest.getElementsByTagName("activity")
        val names = (0 until activities.length).map { (activities.item(it) as Element).getAttributeNS(androidNs, "name") }
        assertTrue("app.loma.android.MainActivity" in names, "activities: $names")
    }
}
