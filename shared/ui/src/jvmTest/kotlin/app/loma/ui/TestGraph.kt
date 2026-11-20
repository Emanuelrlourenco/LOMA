// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import app.loma.data.SalonRepository
import app.loma.data.buildLoma
import app.loma.data.lomaDatabaseBuilder
import app.loma.platform.MessagingApp
import app.loma.platform.MessagingLauncher
import app.loma.platform.PhotoStore
import app.loma.platform.AuthKind
import app.loma.platform.DeviceAuth
import app.loma.platform.KeyValueStore
import app.loma.ui.lock.AppLock
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Abrir WhatsApp/SMS de mentira: só registra o que foi pedido. */
class FakeMessaging(var apps: Set<MessagingApp> = setOf(MessagingApp.WHATSAPP, MessagingApp.SMS)) : MessagingLauncher {
    data class Opened(val app: MessagingApp, val phone: String, val text: String)
    val opened = mutableListOf<Opened>()
    val calls = mutableListOf<String>()
    override fun available(): Set<MessagingApp> = apps
    override fun open(app: MessagingApp, phoneDigits: String, text: String): Boolean = opened.add(Opened(app, phoneDigits, text))
    override fun call(phoneDigits: String): Boolean = calls.add(phoneDigits)
}

/** Fotos de mentira em memória. */
class FakePhotos : PhotoStore {
    val files = mutableMapOf<String, ByteArray>()
    val deleted = mutableListOf<String>()
    override suspend fun importPhoto(sourceUri: String): String = "foto-${files.size}.jpg".also { files[it] = ByteArray(0) }
    override suspend fun read(file: String): ByteArray? = files[file]
    override suspend fun delete(file: String) {
        deleted += file
        files.remove(file)
    }
}

/** Ajustes síncronos em memória. */
class FakeKeyValueStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String): String? = values[key]
    override fun put(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}

/** Digital e bloqueio do celular de mentira: registra os pedidos e responde [result]. */
class FakeDeviceAuth(var biometric: Boolean = true, var secure: Boolean = true, var result: Boolean = true) : DeviceAuth {
    val calls = mutableListOf<AuthKind>()
    override fun biometricAvailable(): Boolean = biometric
    override fun deviceSecure(): Boolean = secure
    override suspend fun authenticate(kind: AuthKind, title: String, subtitle: String, cancel: String): Boolean {
        calls += kind
        return result
    }
}

/** Relógio que o teste pode adiantar. */
class TestClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

val SaoPaulo: TimeZone = TimeZone.of("America/Sao_Paulo")

fun saoPaulo(day: Int, hour: Int, minute: Int = 0, month: Int = 11): Instant =
    LocalDateTime(2026, month, day, hour, minute).toInstant(SaoPaulo)

/** Grafo com banco de verdade num arquivo temporário, fuso de São Paulo e relógio em 20/11/2026 10:00. */
class TestGraph(now: Instant = saoPaulo(20, 10), pin: String? = null) {
    private val file = File.createTempFile("loma-ui", ".db").also { it.delete(); it.deleteOnExit() }
    val clock = TestClock(now)
    val repository = SalonRepository(lomaDatabaseBuilder(file).buildLoma(), clock)
    val messaging = FakeMessaging()
    val photos = FakePhotos()
    val store = FakeKeyValueStore()
    val deviceAuth = FakeDeviceAuth()

    // Com [pin], o app "abre do zero" já com o PIN ligado (bloqueado).
    init {
        if (pin != null) kotlinx.coroutines.runBlocking { AppLock(store, clock, iterations = 1_000).setPin(pin) }
    }

    val lock = AppLock(store, clock, iterations = 1_000)
    val graph = LomaGraph(repository, messaging, photos, clock, lock, deviceAuth) { SaoPaulo }
}

/** PNG de verdade (degradê menta → lilás) para testar e capturar fotos. */
fun testPng(size: Int = 96): ByteArray {
    val img = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) for (y in 0 until size) {
        val t = (x + y).toFloat() / (2 * size)
        val r = (0x5E + (0xB6 - 0x5E) * t).toInt(); val gr = (0xD6 + (0x9C - 0xD6) * t).toInt(); val b = (0xB4 + (0xF2 - 0xB4) * t).toInt()
        img.setRGB(x, y, (r shl 16) or (gr shl 8) or b)
    }
    return java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(img, "png", it) }.toByteArray()
}
