// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

/** PBKDF2-HMAC-SHA256 da plataforma (javax.crypto / CommonCrypto). */
expect fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int = 32): ByteArray

/** Bytes de um gerador criptográfico da plataforma. */
expect fun secureRandomBytes(size: Int): ByteArray
