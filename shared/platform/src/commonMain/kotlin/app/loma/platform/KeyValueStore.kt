// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

/**
 * Ajustes pequenos lidos na hora, sem esperar o banco (o bloqueio precisa saber antes da primeira tela).
 * Android: SharedPreferences privado `loma_seguranca` (fora do backup em nuvem); iOS: NSUserDefaults.
 */
interface KeyValueStore {
    fun get(key: String): String?
    /** `null` apaga a chave. */
    fun put(key: String, value: String?)
}
