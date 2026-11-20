// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui

import app.loma.data.SalonRepository
import app.loma.data.buildLoma
import app.loma.data.lomaDatabaseBuilder
import app.loma.platform.MessagingApp
import app.loma.platform.MessagingLauncher
import app.loma.platform.PhotoStore
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

/** Relógio que o teste pode adiantar. */
class TestClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

val SaoPaulo: TimeZone = TimeZone.of("America/Sao_Paulo")

fun saoPaulo(day: Int, hour: Int, minute: Int = 0, month: Int = 11): Instant =
    LocalDateTime(2026, month, day, hour, minute).toInstant(SaoPaulo)

/** Grafo com banco de verdade num arquivo temporário, fuso de São Paulo e relógio em 20/11/2026 10:00. */
class TestGraph(now: Instant = saoPaulo(20, 10)) {
    private val file = File.createTempFile("loma-ui", ".db").also { it.delete(); it.deleteOnExit() }
    val clock = TestClock(now)
    val repository = SalonRepository(lomaDatabaseBuilder(file).buildLoma(), clock)
    val messaging = FakeMessaging()
    val photos = FakePhotos()
    val graph = LomaGraph(repository, messaging, photos, clock) { SaoPaulo }
}
