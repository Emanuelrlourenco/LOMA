// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import app.loma.ui.FakeKeyValueStore
import app.loma.ui.TestClock
import app.loma.ui.saoPaulo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

class AppLockTest {
    private val clock = TestClock(saoPaulo(20, 10))
    private val store = FakeKeyValueStore()
    private fun lock() = AppLock(store, clock, iterations = 1_000)

    @Test
    fun semPinNaoBloqueia() {
        val l = lock()
        assertFalse(l.state.value.enabled)
        assertFalse(l.state.value.locked)
    }

    @Test
    fun abrirDoZeroComPinBloqueia() = runBlocking {
        lock().setPin("1234")
        val reaberto = lock()
        assertTrue(reaberto.state.value.enabled)
        assertTrue(reaberto.state.value.locked)
        assertEquals(60, reaberto.state.value.timeoutSeconds)
    }

    @Test
    fun pinGuardadoComoHash() = runBlocking {
        val l = lock()
        l.setPin("1234")
        assertFalse(l.state.value.locked)
        val salvo = store.get("pin")!!
        assertTrue(salvo.startsWith("pbkdf2-sha256\$1000\$"), salvo)
        assertFalse("1234" in salvo)
    }

    @Test
    fun pinCertoDesbloqueia() = runBlocking {
        lock().setPin("1234")
        val l = lock()
        assertEquals(PinCheck.Ok, l.check("1234"))
        assertFalse(l.state.value.locked)
    }

    @Test
    fun cincoErrosEsperam30s() = runBlocking {
        lock().setPin("1234")
        val l = lock()
        (4 downTo 1).forEach { assertEquals(PinCheck.Wrong(it), l.check("0000")) }
        assertEquals(PinCheck.Wait(30), l.check("0000"))
        clock.now += 10.seconds
        assertEquals(PinCheck.Wait(20), l.check("1234"))
        assertTrue(l.state.value.locked)
        clock.now += 20.seconds
        assertEquals(PinCheck.Ok, l.check("1234"))
    }

    @Test
    fun acertoZeraOsErros() = runBlocking {
        lock().setPin("1234")
        val l = lock()
        repeat(4) { l.check("0000") }
        assertEquals(PinCheck.Ok, l.check("1234"))
        assertEquals(PinCheck.Wrong(4), l.check("0000"))
    }

    @Test
    fun esperaSobreviveAReabrir() = runBlocking {
        lock().setPin("1234")
        val l = lock()
        repeat(5) { l.check("0000") }
        assertEquals(PinCheck.Wait(30), lock().check("1234"))
    }

    @Test
    fun segundoPlanoRespeitaTempo() = runBlocking {
        val l = lock()
        l.setPin("1234")
        l.onBackground(); clock.now += 59.seconds; l.onForeground()
        assertFalse(l.state.value.locked)
        l.onBackground(); clock.now += 61.seconds; l.onForeground()
        assertTrue(l.state.value.locked)

        l.unlock(); l.setTimeout(0)
        l.onBackground(); l.onForeground()
        assertTrue(l.state.value.locked)

        l.unlock(); l.setTimeout(300)
        l.onBackground(); clock.now -= 10.minutes; l.onForeground()
        assertTrue(l.state.value.locked, "relógio voltou: bloqueia por segurança")
    }

    @Test
    fun digitalETempoFicamSalvos() = runBlocking {
        val l = lock()
        l.setPin("1234")
        l.setBiometric(true)
        l.setTimeout(300)
        val reaberto = lock()
        assertTrue(reaberto.state.value.biometric)
        assertEquals(300, reaberto.state.value.timeoutSeconds)
    }

    @Test
    fun desligarApagaPin() = runBlocking {
        val l = lock()
        l.setPin("1234")
        l.setBiometric(true)
        l.disable()
        assertNull(store.get("pin"))
        assertFalse(l.state.value.enabled)
        assertFalse(lock().state.value.locked)
    }
}
