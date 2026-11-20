// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

/** BIOMETRIC: digital/rosto. DEVICE: o bloqueio do celular (senha, padrão ou digital) — usado no "Esqueci o PIN". */
enum class AuthKind { BIOMETRIC, DEVICE }

/** Autenticação pelo próprio aparelho; nada sai do celular. */
interface DeviceAuth {
    /** Há digital/rosto cadastrado e utilizável. */
    fun biometricAvailable(): Boolean

    /** O celular tem bloqueio de tela (senha, padrão ou PIN do aparelho). */
    fun deviceSecure(): Boolean

    /** Mostra o pedido do sistema; `true` só se a pessoa se autenticou. Cancelar ou erro = `false`. */
    suspend fun authenticate(kind: AuthKind, title: String, subtitle: String, cancel: String): Boolean
}
