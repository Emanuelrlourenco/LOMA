// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import app.loma.platform.KeyValueStore
import app.loma.platform.pbkdf2Sha256
import app.loma.platform.secureRandomBytes
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class LockState(val enabled: Boolean, val locked: Boolean, val biometric: Boolean, val timeoutSeconds: Int)

sealed interface PinCheck {
    data object Ok : PinCheck
    data class Wrong(val attemptsLeft: Int) : PinCheck
    data class Wait(val seconds: Int) : PinCheck
}

/**
 * Bloqueio do app (design §7.12a). O PIN fica como PBKDF2-HMAC-SHA256 com sal; tudo no [KeyValueStore],
 * lido na hora — o app sabe se abre bloqueado antes da primeira tela.
 */
class AppLock(private val store: KeyValueStore, private val clock: Clock, private val iterations: Int = 120_000) {
    private val _state = MutableStateFlow(read(locked = true))
    val state: StateFlow<LockState> = _state.asStateFlow()
    private var backgroundAt: Instant? = null

    private fun read(locked: Boolean): LockState {
        val enabled = store.get(PIN) != null
        return LockState(
            enabled = enabled,
            locked = enabled && locked,
            biometric = enabled && store.get(BIOMETRIC) == "1",
            timeoutSeconds = store.get(TIMEOUT)?.toIntOrNull() ?: DEFAULT_TIMEOUT,
        )
    }

    /** Liga (ou troca) o PIN e desbloqueia. */
    suspend fun setPin(pin: String) {
        val salt = secureRandomBytes(SALT_SIZE)
        val hash = derive(pin, salt, iterations, HASH_SIZE)
        store.put(PIN, listOf(FORMAT, iterations.toString(), salt.toHex(), hash.toHex()).joinToString("$"))
        store.put(ERRORS, null)
        store.put(WAIT_UNTIL, null)
        _state.value = read(locked = false)
    }

    suspend fun check(pin: String): PinCheck {
        val parts = store.get(PIN)?.split("$") ?: return PinCheck.Ok
        waitSeconds().takeIf { it > 0 }?.let { return PinCheck.Wait(it) }
        val ok = parts.size == 4 && parts[0] == FORMAT && run {
            val expected = parts[3].fromHex()
            constantTimeEquals(derive(pin, parts[2].fromHex(), parts[1].toInt(), expected.size), expected)
        }
        if (ok) {
            store.put(ERRORS, null)
            unlock()
            return PinCheck.Ok
        }
        val errors = (store.get(ERRORS)?.toIntOrNull() ?: 0) + 1
        if (errors >= MAX_ERRORS) {
            store.put(ERRORS, null)
            store.put(WAIT_UNTIL, (clock.now() + WAIT.seconds).toEpochMilliseconds().toString())
            return PinCheck.Wait(WAIT)
        }
        store.put(ERRORS, errors.toString())
        return PinCheck.Wrong(MAX_ERRORS - errors)
    }

    /** Segundos que faltam da espera depois de 5 erros (0 = pode tentar). */
    fun waitSeconds(): Int {
        val until = store.get(WAIT_UNTIL)?.toLongOrNull() ?: return 0
        val left = until - clock.now().toEpochMilliseconds()
        if (left <= 0) {
            store.put(WAIT_UNTIL, null)
            return 0
        }
        // Relógio que voltou não aumenta a espera além de 30 s.
        return ((left + 999) / 1000).toInt().coerceAtMost(WAIT)
    }

    fun disable() {
        listOf(PIN, BIOMETRIC, ERRORS, WAIT_UNTIL).forEach { store.put(it, null) }
        _state.value = read(locked = false)
    }

    fun setBiometric(on: Boolean) {
        store.put(BIOMETRIC, if (on) "1" else null)
        _state.update { it.copy(biometric = it.enabled && on) }
    }

    fun setTimeout(seconds: Int) {
        store.put(TIMEOUT, seconds.toString())
        _state.update { it.copy(timeoutSeconds = seconds) }
    }

    /** Depois da digital ou do PIN certo. */
    fun unlock() = _state.update { it.copy(locked = false) }

    fun onBackground() {
        backgroundAt = clock.now()
    }

    fun onForeground() {
        val at = backgroundAt ?: return
        backgroundAt = null
        val s = _state.value
        if (!s.enabled || s.locked) return
        val away = clock.now() - at
        if (away.isNegative() || away >= s.timeoutSeconds.seconds) _state.update { it.copy(locked = true) }
    }

    private suspend fun derive(pin: String, salt: ByteArray, rounds: Int, size: Int): ByteArray =
        withContext(Dispatchers.Default) { pbkdf2Sha256(pin.encodeToByteArray(), salt, rounds, size) }

    companion object {
        const val PIN = "pin"
        const val BIOMETRIC = "digital"
        const val TIMEOUT = "tempo"
        const val ERRORS = "erros"
        const val WAIT_UNTIL = "espera_ate"
        const val DEFAULT_TIMEOUT = 60
        const val MAX_ERRORS = 5
        const val WAIT = 30
        private const val FORMAT = "pbkdf2-sha256"
        private const val SALT_SIZE = 16
        private const val HASH_SIZE = 32
    }
}

private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var diff = 0
    for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
    return diff == 0
}

private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

private fun String.fromHex(): ByteArray = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
